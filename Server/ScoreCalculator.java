package Server;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;


/**
 * Yahtzee oyununda belirli bir zar kombinasyonu için kategoriye göre skoru
 * hesaplayan yardımcı sınıftır.
 */
public class ScoreCalculator {

    // Yahtzee'deki tüm geçerli kategori adlarının küçük harflerle tutulduğu sabit küme.
    private static final Set<String> ALL_CATEGORIES = new HashSet<>(Arrays.asList(
            "ones", "twos", "threes", "fours", "fives", "sixes", // Üst bölüm kategorileri
            "three of a kind", "four of a kind", "full house", "small straight", // Alt bölüm kategorileri
            "large straight", "yahtzee", "chance" // Diğer alt bölüm kategorileri
    ));

    /**
     * Verilen zar kombinasyonu için belirtilen kategoriye ait skoru hesaplar.
     *
     * @param category Hesaplama yapılacak kategori adı (büyük/küçük harf duyarsız).
     * @param dice Hesaplama için kullanılacak 5 zarın değerleri (1-6 arası).
     * @return Hesaplanan puan. Kategori geçersizse veya kombinasyon kategoriye
     * uymuyorsa 0 döner (bazı kategoriler için durum farklı olabilir, örneğin "Chance" her zaman toplamı döner).
     */
    public static int calculate(String category, int[] dice) {
        // Zar dizisinin null olup olmadığını veya boyutunun 5 olup olmadığını kontrol et.
        if (dice == null || dice.length != 5) {
            System.err.println("Hata: Geçersiz zar dizisi boyutu: " + (dice != null ? dice.length : "null"));
            return 0; // Geçersiz dizi boyutu durumunda 0 dön.
        }

        // Kategori adını küçük harfe çevirerek karşılaştırmayı kolaylaştır.
        String lowerCategory = category.toLowerCase();
        // Belirtilen kategorinin geçerli kategoriler kümesinde olup olmadığını kontrol et.
        if (!ALL_CATEGORIES.contains(lowerCategory)) {
            System.err.println("Hata: Geçersiz kategori adı: " + category);
            return 0; // Geçersiz kategori adı durumunda 0 dön.
        }

        // Zar değerlerinin bir kopyasını oluştur ve sırala. Bu, düz veya ardışık kontrolleri için faydalıdır.
        int[] sortedDice = Arrays.copyOf(dice, dice.length);
        Arrays.sort(sortedDice);

        // Zar değerlerinin frekansını (her bir zar değerinden kaç tane olduğunu) hesapla.
        // Bu, "of a kind" ve "full house" gibi kategoriler için temeldir.
        Map<Integer, Integer> freq = getFrequencyMap(dice);

        // Kategoriye göre ilgili skor hesaplama metodunu çağır.
        switch (lowerCategory) {
            case "ones":
                return calculateUpperSection(sortedDice, 1);
            case "twos":
                return calculateUpperSection(sortedDice, 2);
            case "threes":
                return calculateUpperSection(sortedDice, 3);
            case "fours":
                return calculateUpperSection(sortedDice, 4);
            case "fives":
                return calculateUpperSection(sortedDice, 5);
            case "sixes":
                return calculateUpperSection(sortedDice, 6);
            case "three of a kind":
                return calculateNOfAKind(freq, 3); // Üçlü kombinasyon kontrolü.
            case "four of a kind":
                return calculateNOfAKind(freq, 4); // Dörtlü kombinasyon kontrolü.
            case "full house":
                return calculateFullHouse(freq); // Full House kontrolü.
            case "small straight":
                return calculateStraight(sortedDice, 4); // Küçük düz (4 ardışık zar) kontrolü.
            case "large straight":
                return calculateStraight(sortedDice, 5); // Büyük düz (5 ardışık zar) kontrolü.
            case "yahtzee":
                return calculateYahtzee(freq); // Yahtzee (beş aynı zar) kontrolü.
            case "chance":
                return calculateChance(sortedDice); // Tüm zarların toplamı.

            default:
                // Bu duruma, kategorinin geçerliliği yukarıda kontrol edildiği için normalde gelinmemelidir.
                return 0;
        }
    }

    /**
     * Üst bölüm kategorileri (Ones, Twos, ... Sixes) için skoru hesaplar.
     * Belirtilen sayıdaki zarların toplamını döndürür. Örneğin, 'fives' kategorisi için,
     * zar dizisindeki tüm 5'lerin toplamını verir.
     *
     * @param dice Hesaplama için kullanılacak zar değerleri.
     * @param number Hesaplama yapılacak zar değeri (1-6 arası).
     * @return Belirtilen sayıdaki zarların toplamı.
     */
    private static int calculateUpperSection(int[] dice, int number) {
        int score = 0;
        for (int die : dice) {
            if (die == number) {
                score += number; // Eşleşen her zar değeri için skora ekle.
            }
        }
        return score;
    }

    /**
     * N Of A Kind kategorileri (Three of a Kind, Four of a Kind) için skoru hesaplar.
     * Belirtilen sayıda aynı zardan (n) varsa, tüm zarların toplamını döndürür. Yoksa 0.
     *
     * @param freq Zar değerlerinin frekans haritası.
     * @param n Gereken minimum aynı zar sayısı (3 veya 4).
     * @return N Of A Kind varsa tüm zarların toplamı, yoksa 0.
     */
    private static int calculateNOfAKind(Map<Integer, Integer> freq, int n) {
        // Frekans haritasındaki her bir sayının (zar değeri) kaç kez geçtiğini kontrol et.
        for (int count : freq.values()) {
            if (count >= n) { // Eğer belirtilen sayı kadar veya daha fazla aynı zardan varsa
                // Tüm zarların toplamını hesapla ve döndür.
                return freq.entrySet().stream().mapToInt(entry -> entry.getKey() * entry.getValue()).sum();
            }
        }
        return 0; // N Of A Kind kombinasyonu yoksa 0 dön.
    }

    /**
     * Full House kategorisi için skoru hesaplar.
     * Zar kombinasyonunda bir üçlü (aynı değerden üç zar) ve bir ikili (aynı değerden iki zar) varsa
     * 25 puan döndürür. Yoksa 0.
     *
     * @param freq Zar değerlerinin frekans haritası.
     * @return Full House varsa 25, yoksa 0.
     */
    private static int calculateFullHouse(Map<Integer, Integer> freq) {
        boolean hasThree = false; // Üçlü olup olmadığını belirten bayrak.
        boolean hasTwo = false; // İkili olup olmadığını belirten bayrak.
        for (int count : freq.values()) {
            if (count == 3) {
                hasThree = true; // Üçlü bulundu.
            }
            if (count == 2) {
                hasTwo = true; // İkili bulundu.
            }
        }
        // Hem üçlü hem de ikili varsa 25 puan dön, aksi takdirde 0.
        return (hasThree && hasTwo) ? 25 : 0;
    }

    /**
     * Straight kategorileri (Small Straight, Large Straight) için skoru hesaplar.
     * Belirtilen uzunlukta (length) ardışık zar dizisi varsa puan döndürür
     * (Small Straight için 30, Large Straight için 40). Yoksa 0.
     * Zar dizisi önceden sıralanmış olmalıdır.
     *
     * @param sortedDice Sıralanmış zar değerleri.
     * @param length Gereken ardışık zar uzunluğu (4 veya 5).
     * @return Straight varsa puan, yoksa 0.
     */
    private static int calculateStraight(int[] sortedDice, int length) {
        // Tekrarlayan zarları kaldırarak benzersiz ve sıralı bir liste oluştur.
        // Bu, düz kombinasyonları kontrol ederken kolaylık sağlar.
        List<Integer> uniqueSortedDice = new ArrayList<>();
        uniqueSortedDice.add(sortedDice[0]);
        for (int i = 1; i < sortedDice.length; i++) {
            if (sortedDice[i] != sortedDice[i - 1]) {
                uniqueSortedDice.add(sortedDice[i]);
            }
        }

        // Benzersiz sıralı listede ardışık diziyi ara.
        int consecutiveCount = 1; // Ardışık zar sayısını tutar.
        int maxConsecutive = 1; // Bulunan en uzun ardışık zar dizisi uzunluğunu tutar.
        for (int i = 1; i < uniqueSortedDice.size(); i++) {
            if (uniqueSortedDice.get(i) == uniqueSortedDice.get(i - 1) + 1) {
                consecutiveCount++; // Ardışık ise sayacı artır.
            } else {
                consecutiveCount = 1; // Ardışık değilse sayacı sıfırla.
            }
            maxConsecutive = Math.max(maxConsecutive, consecutiveCount); // En uzun ardışık diziyi güncelle.
        }

        // Küçük düz (4 ardışık) ve Büyük düz (5 ardışık) için puanları döndür.
        if (length == 4 && maxConsecutive >= 4) {
            return 30; // Small Straight puanı.
        }
        if (length == 5 && maxConsecutive >= 5) {
            return 40; // Large Straight puanı.
        }
        return 0; // Straight kombinasyonu yoksa 0 dön.
    }

    /**
     * Yahtzee kategorisi için skoru hesaplar.
     * Beş zar da aynıysa 50 puan döndürür. Yoksa 0.
     *
     * @param freq Zar değerlerinin frekans haritası.
     * @return Yahtzee varsa 50, yoksa 0.
     */
    private static int calculateYahtzee(Map<Integer, Integer> freq) {
        // Frekans haritasındaki her bir sayının (zar değeri) kaç kez geçtiğini kontrol et.
        for (int count : freq.values()) {
            if (count == 5) { // Eğer bir zar değerinden 5 tane varsa
                return 50; // Yahtzee puanını dön.
            }
        }
        return 0; // Yahtzee kombinasyonu yoksa 0 dön.
    }

    /**
     * Chance kategorisi için skoru hesaplar.
     * Tüm zarların toplamını döndürür. Bu kategori her zaman bir puan sağlar.
     *
     * @param dice Hesaplama için kullanılacak zar değerleri.
     * @return Tüm zarların toplamı.
     */
    private static int calculateChance(int[] dice) {
        int score = 0;
        for (int die : dice) {
            score += die; // Her zar değerini skora ekle.
        }
        return score;
        // Alternatif olarak Java 8 Stream API ile daha kısa yazım: return Arrays.stream(dice).sum();
    }

    /**
     * Zar değerlerinin frekansını hesaplayan yardımcı metod.
     * Her bir zar değerinden kaç tane olduğunu bir Map olarak döndürür.
     *
     * @param dice Frekansı hesaplanacak zar değerleri.
     * @return Zar değeri -> Sayısı şeklinde Map.
     */
    private static Map<Integer, Integer> getFrequencyMap(int[] dice) {
        Map<Integer, Integer> freq = new HashMap<>();
        for (int die : dice) {
            // Zar değerini anahtar, sayısını değer olarak Map'e ekle veya mevcut değerini artır.
            freq.put(die, freq.getOrDefault(die, 0) + 1);
        }
        return freq;
    }
}