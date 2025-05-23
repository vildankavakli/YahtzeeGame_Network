package Server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger; // Atomik işlemler için kullanılır, özellikle sayaçlar için güvenlidir.

/**
 * GameManager sınıfı, Yahtzee oyununun sunucu tarafındaki tüm oyun mantığını yönetir.
 * Oyuncuların girişlerini işler, zar atma, skor hesaplama ve tur yönetimi gibi işlemleri yürütür.
 */
public class GameManager {

    private List<PlayerHandler> players; // Oyuna bağlı tüm oyuncuları (PlayerHandler nesnelerini) tutan liste.
    private GameState gameState; // Oyunun mevcut durumunu (zar değerleri, skor tablosu, mevcut tur vb.) tutan nesne.
    private AtomicInteger restartRequests; // Yeni oyun başlatma isteklerini sayan atomik sayaç.
    private int requiredPlayers; // Oyunun başlaması için gereken oyuncu sayısı (veya başlangıçtaki oyuncu sayısı).
    private boolean gameEnded = false; // Oyunun sona erip ermediğini belirten bayrak.

    /**
     * GameManager sınıfının yapıcı metodudur.
     * Oyun yöneticisini belirtilen oyuncu listesiyle başlatır.
     * @param players Oyuna katılan PlayerHandler nesnelerinin listesi.
     */
    public GameManager(List<PlayerHandler> players) {
        this.players = players; // Oyuncu listesini set et.
        this.requiredPlayers = players.size(); // Gerekli oyuncu sayısını mevcut oyuncu sayısına eşitle.
        this.gameState = new GameState(players.size()); // Oyuncu sayısına göre yeni bir GameState oluştur.
        this.restartRequests = new AtomicInteger(0); // Yeniden başlatma istek sayacını sıfırla.
        this.gameEnded = false; // Oyunun henüz bitmediğini işaretle.
    }

    /**
     * Oyunu başlatır. İlk oyuncunun sırasını göndererek oyunu başlatır.
     */
    public void startGame() {
        sendTurnInfo(); // İlk tur bilgisini oyunculara gönder.
    }

    /**
     * Oyunun bitip bitmediğini kontrol eder.
     * @return Oyun bittiyse true, aksi takdirde false.
     */
    public boolean isGameOver() {
        return gameState.isGameOver(); // GameState nesnesinin isGameOver metodunu çağırarak durumu öğren.
    }

    /**
     * Oyunun bittiğini tüm bekleyen thread'lere sinyal veren senkronize metod.
     * Bu metod şu an için kullanılmıyor gibi görünse de, gelecekteki senkronizasyon ihtiyaçları için yer tutar.
     */
    private synchronized void signalGameOver() {
        notifyAll(); // Bu nesne üzerinde bekleyen tüm thread'leri uyandır.
    }

    /**
     * Oyuncudan gelen girişi işler.
     * Yalnızca sırası gelen oyuncunun komutlarını kabul eder.
     * @param player Komutu gönderen PlayerHandler nesnesi.
     * @param input Oyuncudan gelen komut dizesi.
     */
    public void handlePlayerInput(PlayerHandler player, String input) {
        int currentPlayerIndex = gameState.getCurrentPlayerIndex(); // Mevcut sıradaki oyuncunun indeksini al.

        // Eğer komutu gönderen oyuncu sıradaki oyuncu değilse, bilgi mesajı gönder ve çık.
        if (player.getPlayerId() != currentPlayerIndex) {
            player.sendMessage("INFO:Sıra sizde değil. Lütfen bekleyin.");
            return;
        }

        input = input.trim(); // Girişin başındaki ve sonundaki boşlukları temizle.
        System.out.println("Oyuncu " + player.getPlayerId() + " komutu: " + input); // Konsola komutu yazdır.

        // Komut "ROLL" ile başlıyorsa zar atma işlemini yap.
        if (input.toUpperCase().startsWith("ROLL")) {
            String[] parts = input.split(":"); // Komutu ":" karakterine göre ayır.
            List<Integer> heldIndices = new ArrayList<>(); // Tutulacak zar indekslerini saklamak için liste.
            if (parts.length > 1) { // Eğer tutulacak zar indeksleri belirtilmişse
                String[] indexStrings = parts[1].split(","); // İndeksleri "," karakterine göre ayır.
                for (String indexStr : indexStrings) {
                    try {
                        int index = Integer.parseInt(indexStr.trim()); // İndeksi tamsayıya çevir.
                        // İndeks geçerli bir aralıktaysa (0-4) listeye ekle.
                        if (index >= 0 && index < 5) {
                            heldIndices.add(index);
                        } else {
                            System.err.println("Hata: ROLL komutunda geçersiz zar indeksi değeri: " + indexStr);
                        }
                    } catch (NumberFormatException e) {
                        // Sayısal dönüşüm hatası olursa konsola yazdır.
                        System.err.println("Hata: ROLL komutunda geçersiz zar indeksi formatı: " + indexStr);
                    }
                }
            }

            int[] currentDiceValues = gameState.getCurrentPlayerDice(); // Mevcut oyuncunun zar değerlerini al.
            if (currentDiceValues == null) { // Eğer henüz zar atılmamışsa, boş bir zar dizisi oluştur.
                currentDiceValues = new int[5];
            }

            // Zar atma işlemini yap ve yeni zar değerlerini al.
            int[] newDiceValues = gameState.rollDice(heldIndices, currentDiceValues);

            // Tüm oyunculara güncel zar değerlerini gönder.
            player.sendMessage("DICE:" + arrayToString(newDiceValues));

        } else if (input.toLowerCase().startsWith("move:")) { // Komut "MOVE:" ile başlıyorsa skor kaydetme işlemini yap.
            try {
                String[] parts = input.substring(5).split(":"); // "MOVE:" kısmını atla ve ":" karakterine göre ayır.
                if (parts.length == 4) { // Beklenen formatta 4 parça olmalı (kategori, zar_değerleri, satır, sütun).
                    String category = parts[0].trim().toLowerCase(); // Kategoriyi al ve küçük harfe çevir.
                    int selectedRow = Integer.parseInt(parts[2]); // Seçilen satır indeksini al.
                    int selectedColumn = Integer.parseInt(parts[3]); // Seçilen sütun indeksini al.

                    int[] diceForScoreCalculation = gameState.getCurrentPlayerDice(); // Mevcut oyuncunun zar değerlerini al.
                    if (diceForScoreCalculation == null) { // Eğer zar atılmamışsa uyarı gönder.
                        player.sendMessage("INFO:Skor hesaplamak için zar atılmış olmalı.");
                        return;
                    }
                    // Seçilen kategoriye göre skoru hesapla.
                    int calculatedScore = ScoreCalculator.calculate(category, diceForScoreCalculation);

                    // Eğer bu kategori daha önce kullanıldıysa uyarı gönder.
                    if (gameState.isCategoryUsed(player.getPlayerId(), category)) {
                        player.sendMessage("INFO:Bu kategori daha önce kullanıldı. Lütfen başka bir kategori seçin.");
                        return;
                    }

                    // Oyuncunun skorunu GameState'e kaydet.
                    gameState.setScore(player.getPlayerId(), category, calculatedScore);
                    player.sendMessage("INFO:Skorunuz (" + category + ": " + calculatedScore + ") kaydedildi.");

                    // Tüm oyunculara güncel skor bilgisini yayınla.
                    broadcast("SCORE:" + selectedRow + "," + selectedColumn + "," + calculatedScore);
                    gameState.nextTurn(); // Bir sonraki tura geç.

                    // Oyunun bitip bitmediğini kontrol et.
                    if (gameState.isGameOver()) {
                        broadcast("INFO:Oyun bitti! Sonuçlar hesaplanıyor...");
                        System.out.println("Oyun bitti! Sonuçlar hesaplanıyor...");

                        int winningPlayerId = -1; // Kazanan oyuncunun ID'si
                        int maxScore = -1; // En yüksek skor
                        List<String> playerResults = new ArrayList<>(); // Oyuncu sonuçlarını tutan liste.

                        // Her oyuncunun toplam skorunu hesapla ve sonuçları hazırla.
                        for (int i = 0; i < players.size(); i++) {
                            int currentPlayerTotalScore = gameState.getTotalScore(i);
                            String playerName = "Oyuncu " + (i + 1);

                            playerResults.add(playerName + ": " + currentPlayerTotalScore + " Puan");
                            System.out.println(playerName + " Toplam Skor: " + currentPlayerTotalScore);

                            // En yüksek skoru ve kazananı belirle.
                            if (currentPlayerTotalScore > maxScore) {
                                maxScore = currentPlayerTotalScore;
                                winningPlayerId = i;
                            } else if (currentPlayerTotalScore == maxScore) {
                                // Beraberlik durumunda -2 ile işaretle.
                                winningPlayerId = -2;
                            }
                        }
                        broadcast("GAME_OVER_SUMMARY:"); // Oyun bitiş özeti mesajını gönder.
                        for (String result : playerResults) {
                            broadcast("INFO:" + result); // Her oyuncunun sonucunu gönder.
                        }

                        // Kazananı veya beraberlik durumunu duyur.
                        if (winningPlayerId == -2) {
                            broadcast("INFO:Oyun berabere bitti!");
                        } else {
                            String winnerName = "Oyuncu " + (winningPlayerId + 1);
                            broadcast("INFO:Kazanan: " + winnerName + " " + maxScore + " Puan ile!");
                        }

                    } else {
                        sendTurnInfo(); // Oyun bitmediyse bir sonraki turun bilgisini gönder.
                    }
                } else {
                    // MOVE komutu formatı hatalıysa uyarı gönder.
                    player.sendMessage("INFO:MOVE komutu formatı hatalı. Beklenen: MOVE:kategori:zar1 zar2 z3 z4 z5:satir:sutun");
                }
            } catch (NumberFormatException e) {
                // Sayısal dönüşüm hatası olursa uyarı gönder ve hatayı yazdır.
                player.sendMessage("INFO:MOVE komutundaki sayısal değerler (satır, sütun) hatalı.");
                System.err.println("NumberFormatException in MOVE handling: " + e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                // Diğer genel hatalar için uyarı gönder ve hatayı yazdır.
                player.sendMessage("INFO:MOVE komutu işlenirken bir hata oluştu.");
                System.err.println("General exception in MOVE handling: " + e.getMessage());
                e.printStackTrace();
            }
        } else if (input.equalsIgnoreCase("QUIT")) { // Komut "QUIT" ise oyuncu bağlantısını kes.
            playerDisconnected(player.getPlayerId()); // Oyuncunun bağlantısının kesildiğini bildir.
        } else {
            // Geçersiz komut ise uyarı gönder.
            player.sendMessage("INFO:Geçersiz komut. 'ROLL' ya da 'MOVE:kategori' kullan.");
        }
    }

    /**
     * Bir oyuncudan yeni oyun isteği geldiğinde çağrılır.
     * Tüm oyunculardan istek geldiğinde yeni bir oyun başlatır.
     * @param playerId Yeni oyun isteyen oyuncunun ID'si.
     */
    public synchronized void requestNewGame(int playerId) {
        if (gameEnded) { // Oyun bitmişse yeni oyun isteğini işle.
            int currentRequests = restartRequests.incrementAndGet(); // Restart isteği sayacını artır.
            System.out.println("Oyuncu " + playerId + " restart istedi. Toplam istek: " + currentRequests);

            // Eğer tüm gerekli oyunculardan istek gelmişse
            if (currentRequests >= requiredPlayers) {
                broadcast("INFO:Tüm oyuncular yeni oyun istedi. Yeni oyun başlıyor. Lütfen bekleyin.");
                resetGame(players); // Oyunu sıfırla.
                startGame(); // Yeni oyunu başlat.
                restartRequests.set(0); // İstek sayacını sıfırla.
                gameEnded = false; // Oyun bitiş bayrağını sıfırla.
            } else {
                // Yeterli istek gelmediyse diğer oyuncuların beklendiğini bildir.
                broadcast("INFO:Oyuncu " + (playerId + 1) + " yeni oyun istedi. Diğer oyuncular bekleniyor... (" + currentRequests + "/" + requiredPlayers + ")");
            }
        }
    }

    /**
     * Oyunu sıfırlar ve yeni bir oyun için hazırlar.
     * Mevcut oyuncu listesini kullanır veya günceller.
     * @param newPlayers Yeni oyun için kullanılacak oyuncu listesi (genellikle mevcut liste).
     */
    public void resetGame(List<PlayerHandler> newPlayers) {
        this.players = newPlayers; // Oyuncu listesini güncelle.
        this.gameState = new GameState(players.size()); // Yeni bir GameState nesnesi oluştur.
        this.restartRequests.set(0); // Yeniden başlatma isteklerini sıfırla.
        this.gameEnded = false; // Oyunun henüz bitmediğini işaretle.
        System.out.println("Oyun sıfırlandı. Yeni tur başlıyor.");
        broadcast("INFO:Oyun sıfırlandı. Yeni bir oyun başlayacak."); // Tüm oyunculara oyunun sıfırlandığını bildir.
    }

    /**
     * Bir oyuncunun bağlantısı kesildiğinde çağrılır.
     * Eğer oyun henüz bitmemişse ve oyuncu sayısı yetersiz hale gelirse oyunu sonlandırır.
     * @param playerId Bağlantısı kesilen oyuncunun ID'si.
     */
    public synchronized void playerDisconnected(int playerId) {
        System.out.println("GameManager: Oyuncu " + playerId + " bağlantısı kesildi. Kalan oyuncular: " + (players.size() - 1));

        // Eğer oyun bitmemişse ve kalan oyuncu sayısı gerekliden az ise oyunu sonlandır.
        if (!gameEnded && players.size() - 1 < requiredPlayers) {
            broadcast("INFO:Bir oyuncu bağlantısı kesildi. Oyun sona erdi. Yeterli oyuncu bekleniyor...");
            signalGameOver(); // Oyunun bittiğini sinyal ver.
        }
    }

    /**
     * Tüm oyuncu bağlantılarını kapatır.
     */
    public void closeAllConnections() {
        // Oluşturulan oyuncu listesi üzerinde dolaşarak her oyuncunun bağlantısını kapat.
        // `new ArrayList<>(players)` ile concurrent modification exception'ı önlenir.
        for (PlayerHandler p : new ArrayList<>(players)) {
            p.closeConnection();
        }
        players.clear(); // Oyuncu listesini temizle.
        System.out.println("Tüm oyuncu bağlantıları kapatıldı.");
    }

    /**
     * Mevcut turun bilgisini (sıradaki oyuncu vb.) tüm oyunculara gönderir.
     */
    private void sendTurnInfo() {
        gameEnded = true; // Oyunun başladığını (veya tur dönüşünün başladığını) işaretle.
        int currentPlayerIndex = gameState.getCurrentPlayerIndex(); // Mevcut sıradaki oyuncunun indeksini al.
        broadcast("INFO:Şu an sıra Oyuncu " + (currentPlayerIndex + 1) + "'da."); // Genel bilgi mesajı yayınla.
        // Sıradaki oyuncuya özel mesaj gönder.
        players.get(currentPlayerIndex).sendMessage("INFO:Sıra sende! Zar atmak için 'ROLL' yaz veya skor seçip 'GÖNDER'e bas.");
        broadcast("TURN:" + currentPlayerIndex); // Tüm oyunculara sıra bilgisini gönder.
    }

    /**
     * Belirtilen mesajı oyundaki tüm oyunculara yayınlar (broadcast).
     * @param message Yayınlanacak mesaj.
     */
    private void broadcast(String message) {
        for (PlayerHandler p : players) {
            p.sendMessage(message); // Her oyuncuya mesajı gönder.
        }
    }

    /**
     * Bir tamsayı dizisini boşluklarla ayrılmış bir dizeye dönüştürür.
     * Zar değerlerini temsil eden diziyi istemciye göndermek için kullanılır.
     * @param arr Dönüştürülecek tamsayı dizisi.
     * @return Tamsayıların boşluklarla ayrılmış dize temsili.
     */
    private String arrayToString(int[] arr) {
        StringBuilder sb = new StringBuilder(); // Dize oluşturmak için StringBuilder kullan.
        for (int i = 0; i < arr.length; i++) {
            sb.append(arr[i]); // Dizinin elemanını ekle.
            if (i < arr.length - 1) { // Son eleman değilse boşluk ekle.
                sb.append(" ");
            }
        }
        return sb.toString(); // Oluşturulan dizeyi döndür.
    }
}