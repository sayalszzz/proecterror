import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RestaurantController {
    private final RestaurantDAO dao;
    public RestaurantController(RestaurantDAO dao) { this.dao = dao; }

    public List<Dish> getAllDishes() { return dao.getAllDishes(); }
    public List<Order> getAllOrders() { return dao.getAllOrders(); }
    public void addDish(Dish dish) { dao.addDish(dish); }

    public Order createOrder(long id, String customerName, String customerPhone, List<Dish> dishes) {
        // ОШИБКА РЕФАКТОРИНГА 5: строка "NEW" является магическим значением статуса.
        Order order = new Order(id, customerName, customerPhone, dishes, "NEW", BigDecimal.ZERO);
        order.updateTotal();
        dao.addOrder(order);
        return order;
    }

    public void changeOrderStatus(Order order, String status) { order.status = status; dao.updateOrderStatus(order.id, status); }

    public List<Dish> filterByCategory(String category) {
        // ОШИБКА ОПТИМИЗАЦИИ 2: линейный поиск без индексирования по категории.
        List<Dish> result = new ArrayList<>();
        for (Dish dish : dao.getAllDishes()) if (dish.category.equals(category)) result.add(dish);
        return result;
    }

    public List<Dish> searchDish(String query) {
        List<Dish> result = new ArrayList<>();
        for (Dish dish : dao.getAllDishes()) {
            // ОШИБКА ОПТИМИЗАЦИИ 3: toLowerCase() многократно вычисляется для каждого элемента.
            // ОШИБКА ОПТИМИЗАЦИИ 4 - дубль п.3
            if (dish.name.toLowerCase().contains(query.toLowerCase()) && dish.name.toLowerCase().startsWith(query.toLowerCase().substring(0, 1))) result.add(dish);
        }
        return result;
    }

    // ОШИБКА РЕФАКТОРИНГА 6: Feature Envy — контроллер напрямую знает детали Order.
    public BigDecimal getDailyStats() {
        BigDecimal sum = BigDecimal.ZERO;
        for (Order order : dao.getAllOrders()) {
            for (Dish dish : order.dishes) sum = sum.add(dish.price);
        }
        return sum;
    }
}
