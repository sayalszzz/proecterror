import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RestaurantDAO {
    // Настройки подключения к вашей базе данных MySQL
    private static final String URL = "jdbc:mysql://localhost:3306/restaurant_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "KJ{t,fysq123";

    public RestaurantDAO() {
        initializeSchema();
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    private void initializeSchema() {
        // Исправлено Оптимизации 6: Добавлены составные индексы (INDEX) по внешним ключам и статусам
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS dishes (" +
                    "id BIGINT PRIMARY KEY, " +
                    "name VARCHAR(255) NOT NULL, " +
                    "category VARCHAR(100) NOT NULL, " +
                    "price DECIMAL(10, 2) NOT NULL, " +
                    "weight INT NOT NULL, " +
                    "calories INT NOT NULL, " +
                    "description TEXT, " +
                    "available TINYINT(1) NOT NULL" +
                    ") ENGINE=InnoDB;");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS orders (" +
                    "id BIGINT PRIMARY KEY, " +
                    "customer_name VARCHAR(255) NOT NULL, " +
                    "customer_phone VARCHAR(50) NOT NULL, " +
                    "status VARCHAR(50) NOT NULL, " +
                    "total DECIMAL(10, 2) NOT NULL, " +
                    "INDEX idx_orders_status (status)" +
                    ") ENGINE=InnoDB;");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS order_items (" +
                    "order_id BIGINT, " +
                    "dish_id BIGINT, " +
                    "FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (dish_id) REFERENCES dishes(id), " +
                    "INDEX idx_order_items_order (order_id)" +
                    ") ENGINE=InnoDB;");

        } catch (SQLException exception) {
            // Исправлено Рефакторинга 8: Пробрасываем ошибку, чтобы приложение знало о сбое
            throw new RuntimeException("Критическая ошибка инициализации таблиц MySQL", exception);
        }
    }
    // Исправлено Рефакторинга 3: Единый метод-маппер для создания Dish. Дублирование кода удалено.
    private Dish mapDish(ResultSet result) throws SQLException {
        return new Dish.Builder()
                .id(result.getLong("id"))
                .name(result.getString("name"))
                .category(result.getString("category"))
                .price(result.getBigDecimal("price")) // В MySQL DECIMAL мапится в BigDecimal
                .nutritionalInfo(result.getInt("weight"), result.getInt("calories"))
                .description(result.getString("description"))
                .available(result.getBoolean("available"))
                .build();
    }

    public void addDish(Dish dish) {
        String sql = "INSERT INTO dishes (id, name, category, price, weight, calories, description, available) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, dish.getId());
            statement.setString(2, dish.getName());
            statement.setString(3, dish.getCategory());
            statement.setBigDecimal(4, dish.getPrice());
            statement.setInt(5, dish.getNutritionalInfo().getWeight());
            statement.setInt(6, dish.getNutritionalInfo().getCalories());
            statement.setString(7, dish.getDescription());
            statement.setBoolean(8, dish.isAvailable());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Ошибка сохранения блюда с ID: " + dish.getId(), exception);
        }
    }

    public List<Dish> getAllDishes() {
        List<Dish> dishes = new ArrayList<>();
        String sql = "SELECT * FROM dishes";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                dishes.add(mapDish(result));
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Ошибка получения списка блюд", exception);
        }
        return dishes;
    }
    public void addOrder(Order order) {
        String orderSql = "INSERT INTO orders (id, customer_name, customer_phone, status, total) VALUES (?, ?, ?, ?, ?)";
        String itemSql = "INSERT INTO order_items (order_id, dish_id) VALUES (?, ?)";

        try (Connection connection = connect()) {
            connection.setAutoCommit(false);

            try (PreparedStatement orderStatement = connection.prepareStatement(orderSql);
                 PreparedStatement itemStatement = connection.prepareStatement(itemSql)) {

                orderStatement.setLong(1, order.getId());
                orderStatement.setString(2, order.getCustomer().getName());
                orderStatement.setString(3, order.getCustomer().getPhone());
                orderStatement.setString(4, order.getStatus());
                orderStatement.setBigDecimal(5, order.getTotal());
                orderStatement.executeUpdate();

                // Использование Batch для ускорения пакетной вставки позиций
                for (Dish dish : order.getDishes()) {
                    itemStatement.setLong(1, order.getId());
                    itemStatement.setLong(2, dish.getId());
                    itemStatement.addBatch();
                }
                itemStatement.executeBatch();

                connection.commit(); // Подтверждаем транзакцию
            } catch (SQLException exception) {
                connection.rollback(); // Откатываем все изменения при сбое
                throw exception;
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Ошибка выполнения транзакции создания заказа №" + order.getId(), exception);
        }
    }

    public List<Order> getAllOrders() {
        // Исправлено Оптимизации 10 (Решение N+1) и Рефакторинга 4: Один плоский JOIN запрос за 1 раз.
        String sql = "SELECT o.id AS order_id, o.customer_name, o.customer_phone, o.status AS order_status, o.total AS order_total, " +
                "d.id AS dish_id, d.name, d.category, d.price, d.weight, d.calories, d.description, d.available " +
                "FROM orders o " +
                "LEFT JOIN order_items oi ON o.id = oi.order_id " +
                "LEFT JOIN dishes d ON oi.dish_id = d.id";

        Map<Long, Order.Builder> orderBuilders = new LinkedHashMap<>();

        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                long orderId = result.getLong("order_id");

                if (!orderBuilders.containsKey(orderId)) {
                    Order.Builder builder = new Order.Builder()
                            .id(orderId)
                            .customer(result.getString("customer_name"), result.getString("customer_phone"))
                            .status(result.getString("order_status"));
                    orderBuilders.put(orderId, builder);
                }

                long dishId = result.getLong("dish_id");
                if (dishId != 0) {
                    Dish dish = new Dish.Builder()
                            .id(dishId)
                            .name(result.getString("name"))
                            .category(result.getString("category"))
                            .price(result.getBigDecimal("price"))
                            .nutritionalInfo(result.getInt("weight"), result.getInt("calories"))
                            .description(result.getString("description"))
                            .available(result.getBoolean("available"))
                            .build();

                    orderBuilders.get(orderId).dish(dish);
                }
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Ошибка загрузки истории заказов через JOIN", exception);
        }

        return orderBuilders.values().stream().map(Order.Builder::build).toList();
    }

    public void updateOrderStatus(long id, String status) {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setLong(2, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Ошибка обновления статуса для заказа №" + id, exception);
        }
    }

    public int unsafeCountByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM orders WHERE status = ?";
        // Исправлено Оптимизации 8: Автоматическое закрытие ресурсов через try-with-resources (нет утечек)
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, status);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : 0;
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Ошибка подсчета заказов со статусом: " + status, exception);
        }
    }
} // Конец класса RestaurantDAO
