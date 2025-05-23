package Client;

import java.io.*;
import java.net.*;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class ClientConnection {

    private Socket socket; // Sunucuya bağlanmak için kullanılan soket
    private BufferedReader in; // Sunucudan gelen mesajları okumak için kullanılan okuyucu
    private PrintWriter out; // Sunucuya mesaj göndermek için kullanılan yazıcı
    private GameGUI gui; // Oyunun grafik arayüzü (GUI) nesnesi

    // ClientConnection sınıfının yapıcı metodu
    public ClientConnection(String serverIP, int port, GameGUI gui) {
        this.gui = gui; // GUI referansını ayarla

        try {
            // Belirtilen IP adresi ve porta sahip sunucuya bağlan
            socket = new Socket(serverIP, port);
            // Soketin giriş akışından mesajları okumak için BufferedReader oluştur
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // Soketin çıkış akışına mesaj yazmak için PrintWriter oluştur (auto-flush açık)
            out = new PrintWriter(socket.getOutputStream(), true);
            // GUI'ye bu bağlantı nesnesini set et
            this.gui.setConnection(this);

            // Sunucudan gelen mesajları dinlemek için yeni bir thread başlat
            new Thread(() -> listenForMessages()).start();

            System.out.println("Sunucuya bağlanıldı: " + serverIP);
        } catch (IOException e) {
            // Bağlantı hatası oluşursa hatayı yazdır
            e.printStackTrace();
        }
    }

    // Sunucudan gelen mesajları dinleyen metod
    private void listenForMessages() {
        try {
            String message;
            // Sunucudan mesaj gelmeye devam ettiği sürece döngüyü sürdür
            while ((message = in.readLine()) != null) {
                System.out.println("[SERVER] " + message); // Gelen mesajı konsola yazdır

                // Mesaj "DICE:" ile başlıyorsa zar değerlerini güncelle
                if (message.startsWith("DICE:")) {
                    // "DICE:" kısmını atla ve boşluklara göre ayır
                    String[] parts = message.substring(5).split(" ");
                    int[] values = new int[5]; // 5 adet zar değeri için dizi oluştur
                    try {
                        // Her bir parçayı tamsayıya çevir ve diziye kaydet
                        for (int i = 0; i < 5; i++) {
                            values[i] = Integer.parseInt(parts[i]);
                        }
                        // GUI güncellemelerini Swing Event Dispatch Thread (EDT) üzerinde çalıştır
                        SwingUtilities.invokeLater(() -> {
                            gui.updateDice(values); // Zar değerlerini GUI'de güncelle
                        });
                    } catch (NumberFormatException e) {
                        // Sayısal dönüşüm hatası olursa konsola yazdır
                        System.err.println("Hata: Sunucudan gelen DICE mesajındaki değerler sayı değil: " + message);
                    }

                } else if (message.startsWith("TURN:")) { // Mesaj "TURN:" ile başlıyorsa sıra bilgisini güncelle
                    try {
                        // "TURN:" kısmını atla, boşlukları temizle ve tamsayıya çevir
                        int index = Integer.parseInt(message.substring(5).trim());
                        // GUI güncellemelerini Swing Event Dispatch Thread (EDT) üzerinde çalıştır
                        SwingUtilities.invokeLater(() -> {
                            gui.updateTurn(index); // Sıra bilgisini GUI'de güncelle
                        });
                    } catch (NumberFormatException e) {
                        // Sayısal dönüşüm hatası olursa konsola yazdır
                        System.err.println("Hata: Sunucudan gelen TURN mesajındaki değer sayı değil: " + message);
                    }

                } else if (message.startsWith("INDEX:")) { // Mesaj "INDEX:" ile başlıyorsa oyuncu indeksini set et
                    try {
                        // "INDEX:" kısmını atla, boşlukları temizle ve tamsayıya çevir
                        int myIndex = Integer.parseInt(message.substring(6).trim());
                        // GUI güncellemelerini Swing Event Dispatch Thread (EDT) üzerinde çalıştır
                        SwingUtilities.invokeLater(() -> {
                            gui.setMyPlayerIndex(myIndex); // Kendi oyuncu indeksini GUI'ye set et
                        });
                    } catch (NumberFormatException e) {
                        // Sayısal dönüşüm hatası olursa konsola yazdır
                        System.err.println("Hata: Sunucudan gelen INDEX mesajındaki değer sayı değil: " + message);
                    }

                } else if (message.startsWith("SCORE:")) { // Mesaj "SCORE:" ile başlıyorsa skor bilgisini güncelle
                    // "SCORE:" kısmını atla ve virgüllere göre ayır
                    String[] parts = message.substring(6).split(",");
                    if (parts.length == 3) { // Mesajın 3 parçadan oluştuğundan emin ol
                        try {
                            // Satır, sütun ve skor değerlerini tamsayıya çevir
                            int row = Integer.parseInt(parts[0].trim());
                            int col = Integer.parseInt(parts[1].trim());
                            int score = Integer.parseInt(parts[2].trim());

                            // GUI güncellemelerini Swing Event Dispatch Thread (EDT) üzerinde çalıştır
                            SwingUtilities.invokeLater(() -> {
                                gui.addScoreIfNotUsed(row, col, score); // Skoru GUI'ye ekle (eğer kullanılmamışsa)
                            });

                        } catch (NumberFormatException e) {
                            // Sayısal dönüşüm hatası olursa konsola yazdır
                            System.err.println("Hata: Sunucudan gelen SCORE mesajındaki sayısal değerler hatalı: " + message);
                        } catch (Exception e) {
                            // Diğer genel hatalar için yakalama bloğu
                            System.err.println("Hata: Sunucudan gelen SCORE mesajı işlenirken genel hata oluştu: " + message);
                            e.printStackTrace();
                        }
                    } else {
                        // Mesaj formatı hatalıysa konsola yazdır
                        System.err.println("Hata: Sunucudan gelen SCORE mesaj formatı hatalı: " + message);
                    }
                } else if (message.startsWith("GAME_OVER_SUMMARY:")) { // Mesaj oyun bitiş özetini içeriyorsa
                    // GUI güncellemelerini Swing Event Dispatch Thread (EDT) üzerinde çalıştır
                    SwingUtilities.invokeLater(() -> {
                        gui.showGameOverSummary(); // Oyun bitiş özetini göster
                    });

                } else if (message.startsWith("INFO:")) { // Mesaj genel bilgi içeriyorsa
                    String infoMessage = message.substring(5).trim(); // "INFO:" kısmını atla ve boşlukları temizle
                    // Oyun sonu mesajlarını kontrol et
                    if (infoMessage.startsWith("Oyun bitti! Sonuçlar hesaplanıyor...")
                            || infoMessage.startsWith("Oyun berabere bitti!")
                            || infoMessage.startsWith("Kazanan:")) {

                        // GUI güncellemelerini Swing Event Dispatch Thread (EDT) üzerinde çalıştır
                        SwingUtilities.invokeLater(() -> {
                            gui.addGameOverMessage(infoMessage); // Oyun bitiş mesajını GUI'ye ekle
                        });
                    } else if (infoMessage.equals("Yeni oyun başlıyor. Lütfen bekleyin.")) {
                        gui.resetGUIForNewGameRequest(); // Yeni oyun isteği için GUI'yi sıfırla
                    } else {
                        // Diğer bilgi mesajlarını konsola yazdır (GUI'de gösterilmeyenler)
                        SwingUtilities.invokeLater(() -> {
                            System.out.println("INFO (GUI'de gösterilmeyen): " + infoMessage);
                        });
                    }
                }
            }
        } catch (IOException e) {
            // Bağlantı kesilirse hata mesajı göster ve uygulamayı kapat
            System.out.println("Sunucudan bağlantı kesildi.");
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(gui, "Sunucuyla bağlantı kesildi.", "Bağlantı Hatası", JOptionPane.ERROR_MESSAGE);
                gui.dispose(); // GUI penceresini kapat
                System.exit(1); // Uygulamayı sonlandır
            });
        } finally {
            // Bağlantıyı kapat (hata olsa da olmasa da)
            closeConnection();
        }
    }

    // Sunucuya mesaj göndermek için metod
    public void sendMessage(String msg) {
        if (out != null) { // PrintWriter nesnesi null değilse (yani bağlantı açıksa)
            out.println(msg); // Mesajı sunucuya gönder
        } else {
            // Bağlantı kapalıysa hata mesajı yazdır
            System.err.println("Hata: Sunucu bağlantısı kapalı, mesaj gönderilemedi: " + msg);
        }
    }

    // Bağlantıyı kapatmak için metod
    public void closeConnection() {
        try {
            // Soket null değilse ve kapalı değilse
            if (socket != null && !socket.isClosed()) {
                socket.close(); // Soketi kapat
                System.out.println("Bağlantı kapatıldı.");
            }
        } catch (IOException e) {
            // Kapatma sırasında hata oluşursa yazdır
            e.printStackTrace();
        }
    }
}