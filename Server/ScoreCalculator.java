package Server;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Yahtzee oyununda belirli bir zar kombinasyonu için kategoriye göre skoru
 * hesaplar.
 */
public class ScoreCalculator {

    // Yahtzee'deki tüm geçerli kategori adları (küçük harf)
    private static final Set<String> ALL_CATEGORIES = new HashSet<>(Arrays.asList(
            "birler", "ikiler", "üçler", "dörtler", "beşler", "altılar",
            "3 of kind", "four_of_a_kind", "full house", "small_straight",
            "large_straight", "yahtzee", "chance"
    )); // devamını da düzelt

    /**
     * Verilen zar kombinasyonu için belirtilen kategoriye ait skoru hesaplar.
     *
     * @param category Hesaplama yapılacak kategori adı (küçük/büyük harf
     * olabilir).
     * @param dice Hesaplama için kullanılacak 5 zarın değerleri (1-6 arası).
     * @return Hesaplanan puan. Kategori geçersizse veya kombinasyon kategoriye
     * uymuyorsa 0 döner (bazı kategoriler hariç).
     */
    public static int calculate(String category, int[] dice) {
        // Zar dizisinin geçerliliğini kontrol et (5 zar olmalı)
        if (dice == null || dice.length != 5) {
            System.err.println("Hata: Geçersiz zar dizisi boyutu: " + (dice != null ? dice.length : "null"));
            return 0; // Geçersiz zar dizisi
        }

        // Kategori adını küçük harfe çevir ve geçerliliğini kontrol et
        String lowerCategory = category.toLowerCase();
        if (!ALL_CATEGORIES.contains(lowerCategory)) {
            System.err.println("Hata: Geçersiz kategori adı: " + category);
            return 0; // Geçersiz kategori adı
        }

        // Zar değerlerini kopyala ve sırala (bazı hesaplamalar için kolaylık sağlar)
        int[] sortedDice = Arrays.copyOf(dice, dice.length);
        Arrays.sort(sortedDice);

        // Zar değerlerinin frekansını hesapla (birçok kategori için gerekli)
        Map<Integer, Integer> freq = getFrequencyMap(dice);

        // Kategoriye göre skoru hesapla
        switch (lowerCategory) {
            // Üst Kısım Kategorileri
            case "bir":
                return calculateUpperSection(sortedDice, 1);
            case "iki":
                return calculateUpperSection(sortedDice, 2);
            case "üçler":
                return calculateUpperSection(sortedDice, 3);
            case "fours":
                return calculateUpperSection(sortedDice, 4);
            case "fives":
                return calculateUpperSection(sortedDice, 5);
            case "sixes":
                return calculateUpperSection(sortedDice, 6);

            // Alt Kısım Kategorileri
            case "3 of kind":
                return calculateNOfAKind(freq, 3);
            case "four_of_a_kind":
                return calculateNOfAKind(freq, 4);
            case "full house":
                return calculateFullHouse(freq);
            case "small_straight":
                return calculateStraight(sortedDice, 4); // 4 ardışık zar
            case "large_straight":
                return calculateStraight(sortedDice, 5); // 5 ardışık zar
            case "yahtzee":
                return calculateYahtzee(freq);
            case "chance":
                return calculateChance(sortedDice); // Tüm zarların toplamı

            default:
                // Bu duruma gelinmemeli çünkü kategori geçerliliği yukarıda kontrol edildi
                return 0;
        }
    }

    /**
     * Üst kısım kategorileri için skoru hesaplar (Ones, Twos, ... Sixes).
     * Belirtilen sayıdaki zarların toplamını döndürür.
     *
     * @param dice Hesaplama için kullanılacak zar değerleri.
     * @param number Hesaplama yapılacak zar değeri (1-6).
     * @return Belirtilen sayıdaki zarların toplamı.
     */
    private static int calculateUpperSection(int[] dice, int number) {
        int score = 0;
        for (int die : dice) {
            if (die == number) {
                score += number;
            }
        }
        return score;
    }

    /**
     * N Of A Kind kategorileri için skoru hesaplar (Three of a Kind, Four of a
     * Kind). Belirtilen sayıda aynı zardan varsa, tüm zarların toplamını
     * döndürür. Yoksa 0.
     *
     * @param freq Zar değerlerinin frekans haritası.
     * @param n Gereken minimum aynı zar sayısı (3 veya 4).
     * @return N Of A Kind varsa tüm zarların toplamı, yoksa 0.
     */
    private static int calculateNOfAKind(Map<Integer, Integer> freq, int n) {
        for (int count : freq.values()) {
            if (count >= n) {
                // N Of A Kind varsa, tüm zarların toplamını döndür
                return freq.entrySet().stream().mapToInt(entry -> entry.getKey() * entry.getValue()).sum();
            }
        }
        return 0; // N Of A Kind yok
    }

    /**
     * Full House kategorisi için skoru hesaplar. Bir üçlü ve bir ikili varsa 25
     * puan döndürür. Yoksa 0.
     *
     * @param freq Zar değerlerinin frekans haritası.
     * @return Full House varsa 25, yoksa 0.
     */
    private static int calculateFullHouse(Map<Integer, Integer> freq) {
        boolean hasThree = false;
        boolean hasTwo = false;
        for (int count : freq.values()) {
            if (count == 3) {
                hasThree = true;
            }
            if (count == 2) {
                hasTwo = true;
            }
        }
        return (hasThree && hasTwo) ? 25 : 0;
    }

    /**
     * Straight kategorileri için skoru hesaplar (Small Straight, Large
     * Straight). Belirtilen uzunlukta ardışık zar dizisi varsa puan döndürür
     * (Small: 30, Large: 40). Yoksa 0. Zar dizisi önceden sıralanmış olmalıdır.
     *
     * @param sortedDice Sıralanmış zar değerleri.
     * @param length Gereken ardışık zar uzunluğu (4 veya 5).
     * @return Straight varsa puan, yoksa 0.
     */
    private static int calculateStraight(int[] sortedDice, int length) {
        // Tekrarlayan zarları kaldırarak benzersiz sıralı bir liste oluştur
        List<Integer> uniqueSortedDice = new ArrayList<>();
        uniqueSortedDice.add(sortedDice[0]);
        for (int i = 1; i < sortedDice.length; i++) {
            if (sortedDice[i] != sortedDice[i - 1]) {
                uniqueSortedDice.add(sortedDice[i]);
            }
        }

        // Benzersiz sıralı listede ardışık diziyi ara
        int consecutiveCount = 1;
        int maxConsecutive = 1;
        for (int i = 1; i < uniqueSortedDice.size(); i++) {
            if (uniqueSortedDice.get(i) == uniqueSortedDice.get(i - 1) + 1) {
                consecutiveCount++;
            } else {
                consecutiveCount = 1;
            }
            maxConsecutive = Math.max(maxConsecutive, consecutiveCount);
        }

        if (length == 4 && maxConsecutive >= 4) {
            return 30; // Small Straight
        }
        if (length == 5 && maxConsecutive >= 5) {
            return 40; // Large Straight
        }
        return 0; // Straight yok
    }

    /**
     * Yahtzee kategorisi için skoru hesaplar. Beş zar da aynıysa 50 puan
     * döndürür. Yoksa 0.
     *
     * @param freq Zar değerlerinin frekans haritası.
     * @return Yahtzee varsa 50, yoksa 0.
     */
    private static int calculateYahtzee(Map<Integer, Integer> freq) {
        for (int count : freq.values()) {
            if (count == 5) {
                return 50; // Beş zar da aynı
            }
        }
        return 0; // Yahtzee yok
    }

    /**
     * Chance kategorisi için skoru hesaplar. Tüm zarların toplamını döndürür.
     *
     * @param dice Hesaplama için kullanılacak zar değerleri.
     * @return Tüm zarların toplamı.
     */
    private static int calculateChance(int[] dice) {
        int score = 0;
        for (int die : dice) {
            score += die;
        }
        return score;
        // Veya Java 8 Stream API ile: return Arrays.stream(dice).sum();
    }

    /**
     * Zar değerlerinin frekansını hesaplayan yardımcı metod.
     *
     * @param dice Frekansı hesaplanacak zar değerleri.
     * @return Zar değeri -> Sayısı şeklinde Map.
     */
    private static Map<Integer, Integer> getFrequencyMap(int[] dice) {
        Map<Integer, Integer> freq = new HashMap<>();
        for (int die : dice) {
            freq.put(die, freq.getOrDefault(die, 0) + 1);
        }
        return freq;
    }
}

// Yahtzee bonusları (ilk Yahtzee'den sonraki 100 puanlık bonuslar)
// Bu bonuslar genellikle ScoreCalculator yerine GameState'in calculateFinalScores
// metodunda veya GameManager'da yönetilir, çünkü oyun durumuna (ilk Yahtzee'nin
// 50 puanla kullanılıp kullanılmadığına) bağlıdır.
// Bu nedenle burada bonus hesaplama mantığı eklenmemiştir.
 /*
        for (Score myPoint : myPoints) {
            if (!myPoint.isButtonChoosen) {
                myPoint.getButton().setText(String.valueOf(DiceUtility.CALCULATE(dices, myPoint.getScore_type())));
            }
        }

*/
