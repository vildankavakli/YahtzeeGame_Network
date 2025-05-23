package Server;

import java.util.*;

/**
 * GameState sınıfı, Yahtzee oyununun mevcut durumunu (kimin sırası olduğu,
 * hangi turda olunduğu, oyuncuların zar değerleri ve skor tabloları gibi
 * bilgileri) tutar ve yönetir.
 */
public class GameState {

    private int currentPlayerIndex; // Şu anki sırası olan oyuncunun indeksi (0'dan başlar).
    private int round; // Oyunun mevcut tur numarası (1'den başlar).
    private int[][] diceValues; // Her oyuncunun son attığı 5 zar değerini tutar. [oyuncuId][5 zar].
    // Oyuncu ID'sine göre, her kategori için kazanılan puanları tutan iç içe bir harita.
    private Map<Integer, Map<String, Integer>> scoreBoard; // OyuncuId -> (KategoriAdı -> Puan).
    private static final int totalCategories = 13; // Yahtzee oyunundaki toplam kategori sayısı.

    /**
     * GameState sınıfının yapıcı metodudur. Yeni bir oyun durumu başlatır.
     *
     * @param playerCount Oyuna katılacak toplam oyuncu sayısı.
     */
    public GameState(int playerCount) {
        currentPlayerIndex = 0; // Oyun her zaman 0. oyuncu ile başlar.
        round = 1; // Oyun her zaman 1. tur ile başlar.
        diceValues = new int[playerCount][5]; // Her oyuncu için 5 zarlık bir yer ayrılır.
        scoreBoard = new HashMap<>(); // Skor tahtası haritasını başlat.

        // Her oyuncu için skor tahtasında boş bir kategori-puan haritası oluşturulur.
        for (int i = 0; i < playerCount; i++) {
            scoreBoard.put(i, new HashMap<>());
        }
    }

    /**
     * Mevcut sırası olan oyuncunun indeksini döndürür.
     *
     * @return Şu anki oyuncunun indeksi.
     */
    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    /**
     * Sırayı bir sonraki oyuncuya geçirir. Eğer tüm oyuncuların sırası
     * gelmişse, turu artırır.
     */
    public void nextTurn() {
        // Sıradaki oyuncuya geçiş yapar (dairesel olarak: son oyuncudan sonra tekrar ilk oyuncuya).
        currentPlayerIndex = (currentPlayerIndex + 1) % scoreBoard.size();
        // Eğer sıra tekrar ilk oyuncuya geldiyse (yani tüm oyuncular oynamışsa), turu artır.
        if (currentPlayerIndex == 0) {
            round++; // Tur numarasını artır.
            System.out.println("Yeni Tur Başladı: " + round); // Sunucu konsoluna yeni tur bilgisini yazdır.
            // Yahtzee'de her tur yeni zarlarla başlar. Zar değerleri ilk zar atıldığında güncellenecektir.
        }
        System.out.println("Sıra Oyuncu " + (currentPlayerIndex + 1) + "'a geçti."); // Sunucu konsoluna sıra değişimini yazdır.
    }

    /**
     * Oyunun mevcut tur numarasını döndürür.
     *
     * @return Mevcut tur numarası.
     */
    public int getRound() {
        return round;
    }

    /**
     * Oyuncunun zar atma işlemini gerçekleştirir. Belirtilen indekslerdeki
     * zarları tutar, diğerlerini yeniden atar.
     *
     * @param heldIndices Tutulacak zar indekslerinin listesi (0-4 arası).
     * @param currentDiceValues Oyuncunun mevcut zar değerleri dizisi.
     * @return Yeniden atma sonrası oluşan yeni zar değerleri dizisi.
     */
    public int[] rollDice(List<Integer> heldIndices, int[] currentDiceValues) {
        Random r = new Random(); // Rastgele sayı üretici.
        int[] newDice; // Yeni zar değerlerini tutacak dizi.

        // Eğer henüz zar atılmamışsa veya mevcut zar değerleri geçersizse (örneğin ilk atışta),
        // tüm zarları sıfırdan at.
        if (currentDiceValues == null || currentDiceValues.length != 5) {
            newDice = new int[5]; // 5 zarlık yeni bir dizi oluştur.
            for (int i = 0; i < 5; i++) {
                newDice[i] = r.nextInt(6) + 1; // Her zarı 1-6 arasında rastgele at.
            }
        } else {
            // Mevcut zar değerlerini kopyala. Tutulacak zarlar bu kopyada kalacak.
            newDice = Arrays.copyOf(currentDiceValues, currentDiceValues.length);

            // Tutulmayan zarları yeniden at.
            for (int i = 0; i < newDice.length; i++) {
                if (!heldIndices.contains(i)) { // Eğer bu zarın indeksi tutulanlar listesinde yoksa
                    newDice[i] = r.nextInt(6) + 1; // Zarı yeniden at.
                }
            }
        }

        // Mevcut oyuncunun zar değerlerini güncel zar değerleriyle değiştir.
        diceValues[currentPlayerIndex] = newDice;
        return newDice; // Yeni zar değerlerini döndür.
    }

    /**
     * Mevcut sıradaki oyuncunun son attığı (veya tuttuğu) zar değerlerini
     * döndürür.
     *
     * @return Mevcut oyuncunun 5 zar değeri dizisi.
     */
    public int[] getCurrentPlayerDice() {
        return diceValues[currentPlayerIndex];
    }

    /**
     * Belirli bir oyuncunun belirli bir kategoriye aldığı skoru kaydeder.
     * Kategori adı küçük harfe çevrilerek tutarlılık sağlanır.
     *
     * @param playerId Skoru kaydedilecek oyuncunun ID'si.
     * @param category Skorun kaydedileceği kategori adı.
     * @param score Kaydedilecek puan.
     */
    public void setScore(int playerId, String category, int score) {
        // Oyuncunun skor haritasına kategori ve puanı ekle.
        scoreBoard.get(playerId).put(category.toLowerCase(), score);
        System.out.println("Oyuncu " + (playerId + 1) + " için skor kaydedildi - Kategori: " + category + ", Skor: " + score); // Sunucu konsoluna bilgi yazdır.
    }

    /**
     * Belirli bir oyuncunun tüm skorlarını (kategoriye göre) döndürür.
     *
     * @param playerId Skorları alınacak oyuncunun ID'si.
     * @return Oyuncunun kategoriye göre puanlarını içeren Map.
     */
    public Map<String, Integer> getScoresForPlayer(int playerId) {
        return scoreBoard.get(playerId);
    }

    /**
     * Belirli bir oyuncunun belirli bir kategoriye daha önce skor girip
     * girmediğini kontrol eder. Bu, bir kategorinin yalnızca bir kez
     * kullanılabileceği kuralını uygulamak için kullanılır.
     *
     * @param playerId Kontrol edilecek oyuncunun ID'si.
     * @param category Kontrol edilecek kategori adı.
     * @return Eğer kategori daha önce kullanılmışsa true, aksi takdirde false.
     */
    public boolean isCategoryUsed(int playerId, String category) {
        Map<String, Integer> playerScores = scoreBoard.get(playerId);
        // Eğer oyuncu skor tahtasında yoksa (ki bu normalde olmamalı) veya kategori daha önce girilmemişse false dön.
        if (playerScores == null) {
            return false;
        }
        // Kategori adını küçük harfe çevirerek map'te olup olmadığını kontrol et.
        return playerScores.containsKey(category.toLowerCase());
    }

    /**
     * Oyunun bitip bitmediğini kontrol eder. Oyun, tüm oyuncular tüm
     * kategorilerini doldurduğunda biter.
     *
     * @return Oyun bittiyse true, aksi takdirde false.
     */
    public boolean isGameOver() {
        // Her bir oyuncunun skor tahtasını kontrol et.
        for (Map<String, Integer> playerScores : scoreBoard.values()) {
            // Eğer herhangi bir oyuncunun doldurduğu kategori sayısı toplam kategori sayısından az ise,
            // oyun henüz bitmemiştir.
            if (playerScores.size() < totalCategories) {
                return false; // Henüz doldurulmamış kategori var.
            }
        }
        return true; // Tüm oyuncular tüm kategorileri doldurmuş, oyun bitti.
    }

    /**
     * Belirli bir oyuncunun toplam skorunu hesaplar ve döndürür. Üst bölüm
     * bonusu ve Yahtzee bonusu gibi özel kuralları uygular.
     *
     * @param playerId Toplam skoru hesaplanacak oyuncunun ID'si.
     * @return Oyuncunun toplam skoru.
     */
    public int getTotalScore(int playerId) {
        Map<String, Integer> playerScores = scoreBoard.get(playerId);
        if (playerScores == null) {
            return 0; // Oyuncu skor tahtasında yoksa (olmamalı) 0 dön.
        }

        int totalScore = 0; // Genel toplam skor.
        int upperSectionSum = 0; // Üst bölüm puanlarının toplamı.
        int yahtzeeCount = 0; // Kaç tane Yahtzee yapıldığını sayar.

        // Oyuncunun kaydedilmiş tüm skorları üzerinde döngü yap.
        for (Map.Entry<String, Integer> entry : playerScores.entrySet()) {
            String category = entry.getKey(); // Kategori adı.
            int score = entry.getValue(); // Kategoriye ait puan.

            if (category.equals("ones") || category.equals("twos") || category.equals("threes")
                    || category.equals("fours") || category.equals("fives") || category.equals("sixes")) {
                upperSectionSum += score;
            }

            // Yahtzee kategorisini kontrol et.
            if (category.equals("yahtzee")) {
                if (score == 50) { // Eğer Yahtzee 50 puan alınmışsa
                    yahtzeeCount++; // Yahtzee sayısını artır.
                }
            }
            // Tüm kategorilerin puanlarını genel toplama ekle.
            totalScore += score;
        }

        // Üst Kısım Bonusu hesaplaması.
        if (upperSectionSum >= 63) { // Yahtzee kuralına göre 63 veya üstü ise 35 bonus puan.
            totalScore += 35;
        }

        return totalScore; // Oyuncunun toplam skorunu döndür.
    }
}
