package Server;

/**
 * Bu sınıf, tek bir zarı (dice) temsil eder ve zarın değerini yönetir. Her bir
 * zarın 1 ile 6 arasında bir değeri olabilir ve bu değer atma işlemiyle
 * rastgele belirlenir.
 *
 * * @author user
 */
public class Dice {

    private int value; // Zarın mevcut değerini tutar (1-6 arası)

    /**
     * Dice sınıfının yapıcı metodudur. Yeni bir zar nesnesi oluşturulduğunda,
     * otomatik olarak bir kez zar atılır ve başlangıç değeri belirlenir.
     */
    public Dice() {
        roll(); // Zar nesnesi oluşturulduğunda ilk zar atışını yap
    }

    /**
     * Zarın değerini manuel olarak ayarlamak için kullanılır. Genellikle test
     * veya belirli bir senaryo için zar değerini sabitlemek amacıyla
     * kullanılabilir.
     *
     * * @param value Ayarlanacak zar değeri (1-6 arası olması beklenir)
     */
    public void setValue(int value) {
        this.value = value;
    }

    /**
     * Zarı rastgele atar ve 1 ile 6 arasında yeni bir değer belirler.
     * Math.random() metodu 0.0 (dahil) ile 1.0 (hariç) arasında bir double
     * değer döndürür. Bu değer 6 ile çarpılır (0.0 - 5.999...), tamsayıya
     * çevrilir (0-5) ve 1 eklenir (1-6).
     */
    public void roll() {
        this.value = (int) (Math.random() * 6) + 1;
    }

    /**
     * Zarın mevcut değerini döndürür.
     *
     * * @return Zarın mevcut değeri (bir tamsayı)
     */
    public int getValue() {
        return this.value;
    }
}
