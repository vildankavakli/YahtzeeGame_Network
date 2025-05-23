package Server;

public class Dice {

    private int value; // Zarın mevcut değerini tutar (1-6 arası)

    public Dice() {
        roll(); // Zar nesnesi oluşturulduğunda ilk zar atışını yap
    }

    public void setValue(int value) {
        this.value = value;
    }

    public void roll() {
        this.value = (int) (Math.random() * 6) + 1; // Zarı rastgele atar ve 1 ile 6 arasında yeni bir değer belirler.
    }

    public int getValue() {
        return this.value;
    }
}
