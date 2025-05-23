package Server;

import java.io.*;
import java.net.*;

/**
 * PlayerHandler sınıfı, sunucu tarafında her bağlı oyuncu için ayrı bir thread olarak çalışır.
 * Oyuncu ile sunucu arasındaki iletişimi yönetir: istemciden mesajları okur ve istemciye mesaj gönderir.
 */
public class PlayerHandler implements Runnable {

    private Socket socket; // Oyuncunun bağlı olduğu soket.
    private BufferedReader in; // İstemciden gelen mesajları okumak için giriş akışı okuyucusu.
    private PrintWriter out; // İstemciye mesaj göndermek için çıkış akışı yazıcısı.
    private int playerId; // Oyuncunun benzersiz kimliği (ID).
    private GameManager gameManager; // Oyunun genel mantığını yöneten GameManager nesnesi referansı.
    private volatile boolean isThreadRunning = false; // Thread'in çalışıp çalışmadığını gösteren bayrak. Volatile olması, farklı thread'ler arasında doğru senkronizasyon sağlar.

    /**
     * PlayerHandler sınıfının yapıcı metodudur.
     * @param socket İstemci ile sunucu arasındaki bağlantıyı temsil eden soket.
     * @param playerId Bu oyuncu işleyicisinin yöneteceği oyuncunun ID'si.
     * @param gameManager Oyunun genel yöneticisi.
     */
    public PlayerHandler(Socket socket, int playerId, GameManager gameManager) {
        this.socket = socket; // Soketi başlat.
        this.playerId = playerId; // Oyuncu ID'sini başlat.
        this.gameManager = gameManager; // GameManager referansını başlat.

        try {
            // Soket üzerinden giriş ve çıkış akışlarını oluştur.
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true); // true, otomatik boşaltma (auto-flush) sağlar.
            // sendMessage("Hoş geldin, Oyuncu " + playerId); // Bu satır genellikle Server sınıfında ilk hoş geldin mesajı için kullanılır.
        } catch (IOException e) {
            e.printStackTrace(); // Giriş/çıkış hatası oluşursa hatayı yazdır.
        }
    }

    /**
     * Bu thread'in çalışıp çalışmadığını kontrol eder.
     * @return Thread çalışıyorsa true, aksi takdirde false.
     */
    public boolean isThreadRunning() {
        return isThreadRunning;
    }

    /**
     * Bu thread'in çalışma durumunu ayarlar.
     * @param isRunning Thread'in yeni çalışma durumu.
     */
    public void setThreadRunning(boolean isRunning) {
        this.isThreadRunning = isRunning;
    }

    /**
     * Oyuncunun ID'sini döndürür.
     * @return Oyuncunun int türündeki ID'si.
     */
    public int getPlayerId() {
        return playerId;
    }

    /**
     * GameManager referansını günceller. Özellikle yeni bir oyun başlatıldığında gerekebilir.
     * @param gameManager Yeni GameManager referansı.
     */
    public void setGameManager(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
     * İstemciye bir mesaj gönderir.
     * @param message İstemciye gönderilecek String mesaj.
     */
    public void sendMessage(String message) {
        // out nesnesinin null olup olmadığını ve soketin kapalı olup olmadığını kontrol ederek güvenli mesaj gönderimi sağlar.
        if (out != null && !socket.isClosed()) {
            out.println(message); // Mesajı istemciye yaz.
        }
    }

    /**
     * Oyuncu bağlantısını güvenli bir şekilde kapatır.
     */
    public void closeConnection() {
        try {
            // Soket null değilse ve kapalı değilse kapat.
            if (socket != null && !socket.isClosed()) {
                System.out.println("Oyuncu " + playerId + " bağlantısı kapatılıyor...");
                socket.close(); // Soketi kapat.
            }
        } catch (IOException e) {
            // Kapatma sırasında hata oluşursa hatayı yazdır.
            System.err.println("Oyuncu " + playerId + " bağlantı kapatılırken hata: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * PlayerHandler thread'inin ana çalışma döngüsüdür.
     * İstemciden gelen mesajları dinler ve ilgili Game Manager metodlarını çağırır.
     */
    @Override
    public void run() {
        isThreadRunning = true; // Thread'in çalıştığını işaretle.
        try {
            String input;
            // İstemciden satır satır mesaj oku. Okunacak başka bir şey kalmadığında (bağlantı kesildiğinde) döngüden çıkılır.
            while ((input = in.readLine()) != null) {
                System.out.println("Oyuncu " + playerId + ": " + input); // Gelen mesajı sunucu konsoluna yazdır.

                // Gelen komutu kontrol et ve ilgili işlemi yap.
                if (input.equalsIgnoreCase("RESTART")) { // "RESTART" komutu gelirse
                    System.out.println("Oyuncu " + playerId + " yeni oyun istedi.");
                    if (gameManager != null) {
                        gameManager.requestNewGame(playerId); // GameManager'a yeni oyun isteğini bildir.
                    }
                } else if (input.equalsIgnoreCase("QUIT")) { // "QUIT" komutu gelirse
                    System.out.println("Oyuncu " + playerId + " oyundan ayrıldı.");
                    if (gameManager != null) {
                        gameManager.playerDisconnected(playerId); // GameManager'a oyuncunun bağlantısının kesildiğini bildir.
                    }
                    break; // Döngüden çık, bağlantı kapatılacak.
                } else { // Diğer komutlar (ROLL, MOVE vb.) için
                    gameManager.handlePlayerInput(this, input); // GameManager'a oyuncunun girişini işlemesi için gönder.
                }
            }
        } catch (IOException e) {
            // Giriş/çıkış hatası oluşursa (genellikle bağlantı kesildiğinde)
            System.out.println("Oyuncu " + playerId + " bağlantısı kesildi (okuma hatası): " + e.getMessage());
            if (gameManager != null) {
                gameManager.playerDisconnected(playerId); // GameManager'a oyuncunun bağlantısının kesildiğini bildir.
            }
        }
        // run() metodunun sonunda (ister normal sonlansın ister hata ile) finally bloğu çalışır.
        finally {
            isThreadRunning = false; // Thread'in durduğunu işaretle.
            closeConnection(); // Bağlantıyı güvenli bir şekilde kapat.
        }
    }
}