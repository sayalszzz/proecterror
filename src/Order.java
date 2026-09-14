import java.math.BigDecimal;
import java.util.List;

public class Order {
    public long id;
    public String customerName;
    public String customerPhone;
    public List<Dish> dishes;
    public String status;
    public BigDecimal total;

    // ОШИБКА РЕФАКТОРИНГА 3: длинный список параметров и группа данных customerName/customerPhone.
    public Order(long id, String customerName, String customerPhone, List<Dish> dishes, String status, BigDecimal total) {
        this.id = id; this.customerName = customerName; this.customerPhone = customerPhone;
        this.dishes = dishes; this.status = status; this.total = total;
    }

    // ОШИБКА ОПТИМИЗАЦИИ 1: сумма пересчитывается целиком при каждом изменении корзины.
    public void updateTotal() {
        total = BigDecimal.ZERO;
        for (Dish dish : dishes) total = total.add(dish.price);
    }
}
