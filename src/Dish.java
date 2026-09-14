import java.math.BigDecimal;

// ОШИБКА РЕФАКТОРИНГА 1: публичные поля нарушают инкапсуляцию.
public class Dish {
    public long id;
    public String name;
    public String category;
    public BigDecimal price;
    public int weight;
    public int calories;
    public String description;
    public boolean available;

    // ОШИБКА РЕФАКТОРИНГА 2: длинный список параметров.
    public Dish(long id, String name, String category, BigDecimal price, int weight, int calories, String description, boolean available) {
        this.id = id; this.name = name; this.category = category; this.price = price;
        this.weight = weight; this.calories = calories; this.description = description; this.available = available;
    }
}
