package Server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections; // 'players' listesini senkronize etmek için kullanılır.
import java.util.List;

/**
 * Server sınıfı, Yahtzee oyununun sunucu uygulamasının ana giriş noktasıdır.
 * İstemci bağlantılarını kabul eder, oyuncuları yönetir ve oyun mantığını (GameManager) başlatır.
 */
public class Server {

    private static final int PORT = 12345; // Sunucunun dinleyeceği port numarası.
    private static final int MAX_PLAYERS = 2; // Oyuna katılabilecek maksimum oyuncu sayısı.
    // Eşzamanlı erişimler için thread-safe bir oyuncu listesi oluşturulur.
    private static List<PlayerHandler> players = Collections.synchronizedList(new ArrayList<>());
    private static GameManager gameManager; // Oyunun tüm kurallarını ve akışını yönetecek nesne.
    private static ServerSocket serverSocket; // Sunucu soketi, istemci bağlantılarını kabul etmek için kullanılır.

    /**
     * Sunucu uygulamasının ana metodudur. Sunucuyu başlatır ve oyun döngüsünü yönetir.
     * @param args Komut satırı argümanları (kullanılmıyor).
     */
    public static void main(String[] args) {
        System.out.println("Sunucu başlatılıyor..."); // Sunucu başlatma mesajı.

        try {
            serverSocket = new ServerSocket(PORT); // Belirtilen port üzerinde yeni bir ServerSocket oluştur.
            System.out.println("Sunucu port " + PORT + " üzerinde dinleniyor."); // Dinleme portunu konsola yazdır.

            // Yeni istemci bağlantılarını kabul etmek için ayrı bir thread başlatılır.
            // Bu, ana thread'in oyun mantığını yönetirken bağlantı beklemeye devam etmesini sağlar.
            new Thread(() -> acceptConnections()).start();

            // Ana oyun döngüsü. Bu döngü, oyuncular bağlanana ve oyun bitene kadar devam eder.
            while (true) {
                // Tüm gerekli oyuncular bağlanana kadar beklemek için senkronize blok kullanılır.
                synchronized (players) {
                    while (players.size() < MAX_PLAYERS) {
                        System.out.println("Oyuncu bekleniyor... (" + players.size() + "/" + MAX_PLAYERS + ")");
                        // 'players' listesi üzerinde bekleyerek yeni bir oyuncunun bağlanmasını bekler.
                        players.wait();
                    }
                }

                System.out.println("Tüm oyuncular bağlandı. Oyun başlatılıyor/sıfırlanıyor...");

                // GameManager'ı ilk kez oluştur veya mevcutsa sıfırla.
                if (gameManager == null) {
                    gameManager = new GameManager(players); // Oyuncularla yeni bir GameManager oluştur.
                } else {
                    // Oyun bittiğinde veya yeni bir oyun istendiğinde GameManager'ı sıfırla.
                    gameManager.resetGame(players);
                }

                // Bağlı her PlayerHandler'a güncel GameManager referansını set et ve thread'lerini başlat.
                for (PlayerHandler player : players) {
                    player.setGameManager(gameManager); // GameManager referansını güncelle.
                    player.sendMessage("INDEX:" + player.getPlayerId()); // Oyuncuya kendi ID'sini gönder.
                    // PlayerHandler thread'ini sadece bir kez başlat (eğer henüz başlamadıysa).
                    if (!player.isThreadRunning()) {
                        new Thread(player).start(); // PlayerHandler'ı ayrı bir thread olarak başlat.
                        player.setThreadRunning(true); // Thread'in çalıştığını işaretle.
                    }
                }

                gameManager.startGame(); // Oyunu başlat (ilk turu veya sıfırlanmış oyunu).

                // Oyunun bitmesini beklemek için GameManager üzerinde senkronize blok kullanılır.
                synchronized (gameManager) {
                    // GameManager, oyun bittiğinde veya yeniden başlatma için yeterli istek geldiğinde
                    // notifyAll() metodunu çağırarak buradaki wait() metodunu sonlandırır.
                    gameManager.wait();
                }

                System.out.println("Oyun turu sona erdi. Yeni oyun istekleri bekleniyor...");
                // Bu noktada oyunun bir turu tamamlanmıştır. Main döngüsü devam eder ve
                // oyuncuların "RESTART" komutları beklenir. Yeterli "RESTART" isteği gelirse,
                // GameManager.requestNewGame() metodu aracılığıyla oyun sıfırlanır ve yeni tur başlar.
            }

        } catch (IOException | InterruptedException e) {
            // Sunucu soketi hataları veya thread kesintileri durumunda hatayı yazdır.
            System.err.println("Sunucu hatası: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Sunucu kapatılırken kaynakların temizlenmesi sağlanır.
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close(); // ServerSocket'i kapat.
                }
                // Uygulama kapanırken bağlı tüm oyuncu bağlantılarını kapat.
                if (gameManager != null) {
                    gameManager.closeAllConnections();
                }
            } catch (IOException e) {
                e.printStackTrace(); // Kapatma sırasında hata oluşursa yazdır.
            }
        }
    }

    /**
     * Yeni istemci bağlantılarını kabul eden yardımcı metod. Ayrı bir thread üzerinde çalışır.
     */
    private static void acceptConnections() {
        // Sunucu soketi açık olduğu sürece yeni bağlantıları dinlemeye devam et.
        while (!serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept(); // Yeni bir istemci bağlantısını kabul et.
                System.out.println("Yeni oyuncu bağlandı: " + clientSocket); // Bağlanan istemcinin bilgilerini yazdır.

                synchronized (players) {
                    // Eğer maksimum oyuncu sayısına henüz ulaşılmadıysa, yeni oyuncuyu ekle.
                    if (players.size() < MAX_PLAYERS) {
                        int playerId = players.size(); // Oyuncuya benzersiz bir ID ata (0'dan başlayarak).
                        // PlayerHandler nesnesini oluştururken GameManager referansı geçici olarak null olabilir,
                        // çünkü asıl atama main döngüsünde setGameManager ile yapılacaktır.
                        PlayerHandler newPlayer = new PlayerHandler(clientSocket, playerId, null);
                        players.add(newPlayer); // Yeni oyuncuyu listeye ekle.
                        newPlayer.sendMessage("INFO:Sunucuya bağlandınız. Diğer oyuncular bekleniyor."); // Oyuncuya hoş geldin mesajı gönder.
                        // Yeni bir oyuncu bağlandığında, 'players.wait()' durumunda olan ana thread'i uyandır.
                        players.notifyAll();
                    } else {
                        // Maksimum oyuncu sayısına ulaşıldıysa, yeni bağlantıyı reddet.
                        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                        out.println("INFO:Sunucu dolu. Lütfen daha sonra tekrar deneyin.");
                        clientSocket.close(); // İstemci soketini kapat.
                        System.out.println("Sunucu dolu, yeni bağlantı reddedildi.");
                    }
                }
            } catch (IOException e) {
                // ServerSocket kapatıldığında veya bir ağ hatası oluştuğunda bu exception fırlayabilir.
                System.err.println("Bağlantı kabul etme hatası: " + e.getMessage());
                // Eğer serverSocket kapatıldıysa, bu thread'in döngüsünden çık.
                if (serverSocket.isClosed()) {
                    System.out.println("ServerSocket kapatıldı, bağlantı kabul etme döngüsü sonlanıyor.");
                    break;
                }
            }
        }
    }

    /**
     * Oyuncu listesinden belirli bir PlayerHandler nesnesini kaldırır.
     * Bu metod genellikle bir oyuncunun bağlantısı kesildiğinde GameManager tarafından çağrılır.
     * @param playerId Kaldırılacak oyuncunun ID'si.
     */
    public static void removePlayer(int playerId) {
        synchronized (players) {
            // Belirtilen ID'ye sahip oyuncuyu listeden kaldır.
            players.removeIf(p -> p.getPlayerId() == playerId);
            System.out.println("Oyuncu " + playerId + " listeden kaldırıldı. Kalan oyuncu sayısı: " + players.size());

            // Eğer oyun devam ederken oyuncu sayısı maksimum oyuncu sayısının altına düşerse,
            // ana thread'i tekrar oyuncu beklemeye alabiliriz.
            if (gameManager != null && !gameManager.isGameOver() && players.size() < MAX_PLAYERS) {
                System.out.println("Oyuncu sayısı yetersiz hale geldi (" + players.size() + "/" + MAX_PLAYERS + "). Yeni oyuncular bekleniyor...");
                // Bu çağrı, ana sunucu thread'ini (players.wait() konumunda olanı) uyandırır.
                // Ana döngü, oyuncu sayısının yetersiz olduğunu fark edecek ve tekrar beklemeye geçecektir.
                players.notifyAll();
            }
        }
    }
}