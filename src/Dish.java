import java.math.BigDecimal;

// ОШИБКА РЕФАКТОРИНГА 1: все поля публичные и нарушают инкапсуляцию.
public class Dish {
    public long id;
    public String name;
    public String category;
    public BigDecimal price;
    public int weight;
    public int calories;
    public String description;
    public boolean available;

    // ОШИБКА РЕФАКТОРИНГА 2: конструктор содержит 8 параметров.
    // ОШИБКА РЕФАКТОРИНГА 7: weight/calories — группа несгруппированных данных.
    public Dish(long id, String name, String category, BigDecimal price, int weight, int calories, String description, boolean available) {
        this.id = id; this.name = name; this.category = category; this.price = price;
        this.weight = weight; this.calories = calories; this.description = description; this.available = available;
    }
}
