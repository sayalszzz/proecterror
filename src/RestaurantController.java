import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RestaurantController {
    public static final String STATUS_NEW = "NEW";
    private final RestaurantDAO dao;

    public RestaurantController(RestaurantDAO dao) {
        this.dao = dao;
    }

    public List<Dish> getAllDishes() { return dao.getAllDishes(); }
    public List<Order> getAllOrders() { return dao.getAllOrders(); }
    public void addDish(Dish dish) { dao.addDish(dish); }

    public void createOrder(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Заказ не может быть null.");
        }
        dao.addOrder(order);
    }

    public void changeOrderStatus(Order order, String status) {
        if (order == null) {
            throw new IllegalArgumentException("Заказ не может быть null.");
        }
        order.setStatus(status);
        dao.updateOrderStatus(order.getId(), status);
    }

    public List<Dish> filterByCategory(String category) {
        if (category == null || category.isBlank()) {
            return new ArrayList<>();
        }

        Map<String, List<Dish>> categoryIndex = dao.getAllDishes().stream()
                .collect(Collectors.groupingBy(dish -> dish.getCategory().toLowerCase()));

        return categoryIndex.getOrDefault(category.toLowerCase(), new ArrayList<>());
    }

    public List<Dish> searchDish(String query) {
        List<Dish> result = new ArrayList<>();
        if (query == null || query.isEmpty()) {
            return dao.getAllDishes();
        }

        String lowerQuery = query.toLowerCase();
        String firstChar = lowerQuery.substring(0, 1);
        List<Dish> allDishes = dao.getAllDishes();

        for (Dish dish : allDishes) {
            String dishNameLower = dish.getName().toLowerCase();
            if (dishNameLower.startsWith(firstChar) && dishNameLower.contains(lowerQuery)) {
                result.add(dish);
            }
        }
        return result;
    }

    public BigDecimal getDailyStats() {
        return dao.getAllOrders().stream()
                .map(Order::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
