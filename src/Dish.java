import java.math.BigDecimal;
import java.util.Objects;

public class Dish {
    private final long id;
    private final String name;
    private final String category;
    private final BigDecimal price;
    private final NutritionalInfo nutritionalInfo;
    private final String description;
    private final boolean available;

    private Dish(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.category = builder.category;
        this.price = builder.price;
        this.nutritionalInfo = builder.nutritionalInfo;
        this.description = builder.description;
        this.available = builder.available;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public BigDecimal getPrice() { return price; }
    public NutritionalInfo getNutritionalInfo() { return nutritionalInfo; }
    public String getDescription() { return description; }
    public boolean isAvailable() { return available; }

    public static class Builder {
        private long id;
        private String name;
        private String category;
        private BigDecimal price;
        private NutritionalInfo nutritionalInfo;
        private String description;
        private boolean available;

        public Builder id(long id) {
            if (id <= 0) throw new IllegalArgumentException("ID должен быть больше 0.");
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Имя не может быть пустым.");
            }
            this.name = name;
            return this;
        }

        public Builder category(String category) {
            if (category == null || category.isBlank()) {
                throw new IllegalArgumentException("Категория не может быть пустой.");
            }
            this.category = category;
            return this;
        }

        public Builder price(BigDecimal price) {
            if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Цена не может быть отрицательной или null.");
            }
            this.price = price;
            return this;
        }

        public Builder nutritionalInfo(int weight, int calories) {
            this.nutritionalInfo = new NutritionalInfo(weight, calories);
            return this;
        }

        public Builder description(String description) {
            this.description = Objects.requireNonNullElse(description, "");
            return this;
        }

        public Builder available(boolean available) {
            this.available = available;
            return this;
        }

        public Dish build() {
            if (name == null || category == null || price == null || nutritionalInfo == null) {
                throw new IllegalStateException("Не все обязательные поля заполнены.");
            }
            return new Dish(this);
        }
    }
}
