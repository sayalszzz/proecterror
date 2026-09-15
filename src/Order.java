import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Order {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final long id;
    private final Customer customer;
    private final List<Dish> dishes;
    private String status;
    private BigDecimal total;

    private Order(Builder builder) {
        this.id = builder.id;
        this.customer = builder.customer;
        this.dishes = new ArrayList<>(builder.dishes);
        this.status = builder.status;
        this.total = builder.total;
    }

    public long getId() { return id; }
    public Customer getCustomer() { return customer; }
    public List<Dish> getDishes() { return Collections.unmodifiableList(dishes); }
    public String getStatus() { return status; }
    public BigDecimal getTotal() { return total; }

    public void setStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Статус не может быть пустым.");
        }
        this.status = status;
    }

    public void addDish(Dish dish) {
        if (dish == null) {
            throw new IllegalArgumentException("Блюдо не может быть null.");
        }
        this.dishes.add(dish);
        this.total = this.total.add(dish.getPrice());
    }

    public void removeDish(Dish dish) {
        if (this.dishes.remove(dish)) {
            this.total = this.total.subtract(dish.getPrice());
        }
    }

    public void applyDiscount(int discountPercent) {
        if (discountPercent < 0 || discountPercent > 100) {
            throw new IllegalArgumentException("Скидка должна быть в диапазоне от 0 до 100.");
        }
        BigDecimal discountFactor = HUNDRED.subtract(BigDecimal.valueOf(discountPercent));
        this.total = this.total.multiply(discountFactor).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    public static class Builder {
        private long id;
        private Customer customer;
        private final List<Dish> dishes = new ArrayList<>();
        private String status = "NEW";
        private BigDecimal total = BigDecimal.ZERO;

        public Builder id(long id) {
            if (id <= 0) throw new IllegalArgumentException("ID должен быть больше 0.");
            this.id = id;
            return this;
        }

        public Builder customer(String name, String phone) {
            this.customer = new Customer(name, phone);
            return this;
        }

        public Builder dish(Dish dish) {
            if (dish != null) {
                this.dishes.add(dish);
                this.total = this.total.add(dish.getPrice());
            }
            return this;
        }

        public Builder status(String status) {
            if (status != null && !status.isBlank()) {
                this.status = status;
            }
            return this;
        }

        public Order build() {
            if (id <= 0) throw new IllegalStateException("ID заказа не указан.");
            if (customer == null) throw new IllegalStateException("Данные клиента не заполнены.");
            return new Order(this);
        }
    }
}
