import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Учебная DAO намеренно содержит ошибки из задания группы 6. */
public class RestaurantDAO {
    private static final String URL = "jdbc:sqlite:restaurant-error.db";

    public RestaurantDAO() {
        initializeSchema();
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    private void initializeSchema() {
        // ОШИБКА РЕФАКТОРИНГА 11: комментарий-заглушка «Настройка таблиц» не объясняет решение.
        // Настройка таблиц
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS dishes (id INTEGER PRIMARY KEY, name TEXT, category TEXT, price TEXT, weight INTEGER, calories INTEGER, description TEXT, available INTEGER)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS orders (id INTEGER PRIMARY KEY, customer_name TEXT, customer_phone TEXT, status TEXT, total TEXT)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS order_items (order_id INTEGER, dish_id INTEGER)");
            // ОШИБКА ОПТИМИЗАЦИИ 4: индексы по order_id, status и времени заказа отсутствуют.
        } catch (SQLException exception) {
            // ОШИБКА РЕФАКТОРИНГА 9: ошибка только печатается, приложение не узнает о сбое.
            System.out.println("Ошибка БД: " + exception.getMessage());
        }
    }

    public void addDish(Dish dish) {
        // ОШИБКИ ОПТИМИЗАЦИИ 2, 11 и РЕФАКТОРИНГА 5: SQL собирается конкатенацией и без PreparedStatement.
        String sql = "INSERT INTO dishes VALUES (" + dish.id + ", '" + dish.name + "', '" + dish.category + "', '" + dish.price + "', " + dish.weight + ", " + dish.calories + ", '" + dish.description + "', " + (dish.available ? 1 : 0) + ")";
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException exception) {
            System.out.println("Ошибка БД: " + exception.getMessage());
        }
    }

    public List<Dish> getAllDishes() {
        // ОШИБКИ ОПТИМИЗАЦИИ 3 и 10: загружаются все строки без пагинации и кэширования.
        List<Dish> dishes = new ArrayList<>();
        try (Connection connection = connect(); Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT * FROM dishes")) {
            while (result.next()) {
                // ОШИБКА РЕФАКТОРИНГА 3: создание Dish продублировано в getAllOrders().
                dishes.add(new Dish(
                        result.getLong("id"),
                        result.getString("name"),
                        result.getString("category"),
                        new BigDecimal(result.getString("price")),
                        result.getInt("weight"),
                        result.getInt("calories"),
                        result.getString("description"),
                        result.getInt("available") == 1
                ));
            }
        } catch (SQLException exception) {
            System.out.println("Ошибка БД: " + exception.getMessage());
        }
        return dishes;
    }

    public void addOrder(Order order) {
        // ОШИБКА ОПТИМИЗАЦИИ 7: заказ и позиции записываются без транзакции.
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO orders VALUES (" + order.id + ", '" + order.customerName + "', '" + order.customerPhone + "', '" + order.status + "', '" + order.total + "')");
            for (Dish dish : order.dishes) {
                statement.executeUpdate("INSERT INTO order_items VALUES (" + order.id + ", " + dish.id + ")");
            }
        } catch (SQLException exception) {
            System.out.println("Ошибка БД: " + exception.getMessage());
        }
    }

    public List<Order> getAllOrders() {
        // ОШИБКА РЕФАКТОРИНГА 4: длинный метод объединяет SQL, вложенную загрузку, маппинг и создание объектов.
        // ОШИБКА ОПТИМИЗАЦИИ 1: N+1 — для каждого заказа выполняется отдельный запрос блюд.
        List<Order> orders = new ArrayList<>();
        try (
                Connection orderConnection = connect();
                Statement orderStatement = orderConnection.createStatement();
                ResultSet orderResult = orderStatement.executeQuery("SELECT * FROM orders")
        ) {
            while (orderResult.next()) {
                long orderId = orderResult.getLong("id");
                String customerName = orderResult.getString("customer_name");
                String customerPhone = orderResult.getString("customer_phone");
                String status = orderResult.getString("status");
                BigDecimal total = new BigDecimal(orderResult.getString("total"));
                List<Dish> dishes = new ArrayList<>();
                String itemSql = "SELECT d.* FROM dishes d "
                        + "JOIN order_items oi ON d.id = oi.dish_id "
                        + "WHERE oi.order_id = " + orderId;
                try (
                        Connection itemConnection = connect();
                        Statement itemStatement = itemConnection.createStatement();
                        ResultSet itemResult = itemStatement.executeQuery(itemSql)
                ) {
                    while (itemResult.next()) {
                        // ОШИБКА РЕФАКТОРИНГА 3: тот же маппинг Dish уже написан в getAllDishes().
                        Dish dish = new Dish(
                                itemResult.getLong("id"),
                                itemResult.getString("name"),
                                itemResult.getString("category"),
                                new BigDecimal(itemResult.getString("price")),
                                itemResult.getInt("weight"),
                                itemResult.getInt("calories"),
                                itemResult.getString("description"),
                                itemResult.getInt("available") == 1
                        );
                        dishes.add(dish);
                    }
                }
                Order order = new Order(
                        orderId,
                        customerName,
                        customerPhone,
                        dishes,
                        status,
                        total
                );
                orders.add(order);
            }
        } catch (SQLException exception) {
            System.out.println("Ошибка БД: " + exception.getMessage());
        }
        return orders;
    }

    public void updateOrderStatus(long id, String status) {
        String sql = "UPDATE orders SET status = '" + status + "' WHERE id = " + id;
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException exception) {
            System.out.println("Ошибка БД: " + exception.getMessage());
        }
    }

    public int unsafeCountByStatus(String status) {
        // ОШИБКА ОПТИМИЗАЦИИ 8: Connection, Statement и ResultSet намеренно не закрываются.
        try {
            Connection connection = connect();
            Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM orders WHERE status = '" + status + "'");
            return result.next() ? result.getInt(1) : 0;
        } catch (SQLException exception) {
            System.out.println("Ошибка БД: " + exception.getMessage());
            return 0;
        }
    }
}
