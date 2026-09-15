import java.math.BigDecimal;
import java.util.List;

public class Order {
    // ОШИБКА РЕФАКТОРИНГА 1: публичные поля нарушают инкапсуляцию.
    public long id;
    public String customerName;
    public String customerPhone;
    public List<Dish> dishes;
    public String status;
    public BigDecimal total;

    // ОШИБКА РЕФАКТОРИНГА 2: конструктор содержит длинный список параметров.
    // ОШИБКА РЕФАКТОРИНГА 7 и 10: customerName/customerPhone — группа несгруппированных данных.
    public Order(long id, String customerName, String customerPhone, List<Dish> dishes, String status, BigDecimal total) {
        this.id = id; this.customerName = customerName; this.customerPhone = customerPhone;
        this.dishes = dishes; this.status = status; this.total = total;
    }

    // ОШИБКА ОПТИМИЗАЦИИ 9: сумма пересчитывается целиком при каждом изменении корзины.
    public void updateTotal() {
        total = BigDecimal.ZERO;
        for (Dish dish : dishes) total = total.add(dish.price);
    }

    // ОШИБКА РЕФАКТОРИНГА 5: магическое число 100 используется в расчете скидки.
    public void applyDiscount(int discountPercent) {
        total = total.multiply(BigDecimal.valueOf(100 - discountPercent)).divide(BigDecimal.valueOf(100));
    }
}
