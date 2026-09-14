import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RestaurantDAO {
    private static final String URL = "jdbc:sqlite:restaurant-error.db";

    public RestaurantDAO() { initializeSchema(); }
    private Connection connect() throws SQLException { return DriverManager.getConnection(URL); }

    private void initializeSchema() {
        // Комментарий-заглушка: Настройка таблиц
        try (Connection c = connect(); Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS dishes (id INTEGER PRIMARY KEY, name TEXT, category TEXT, price TEXT, weight INTEGER, calories INTEGER, description TEXT, available INTEGER)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS orders (id INTEGER PRIMARY KEY, customer_name TEXT, customer_phone TEXT, status TEXT, total TEXT)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS order_items (order_id INTEGER, dish_id INTEGER)");
        } catch (SQLException e) { System.out.println("Ошибка БД: " + e.getMessage()); }
    }

    public void resetDatabase() {
        try (Connection c = connect(); Statement s = c.createStatement()) { s.executeUpdate("DELETE FROM order_items"); s.executeUpdate("DELETE FROM orders"); s.executeUpdate("DELETE FROM dishes"); }
        catch (SQLException e) { System.out.println("Ошибка БД: " + e.getMessage()); }
    }

    public void addDish(Dish dish) {
        // ОШИБКА ОПТИМИЗАЦИИ 3: строковая конкатенация вместо PreparedStatement.
        String sql = "INSERT INTO dishes VALUES (" + dish.id + ", '" + dish.name + "', '" + dish.category + "', '" + dish.price + "', " + dish.weight + ", " + dish.calories + ", '" + dish.description + "', " + (dish.available ? 1 : 0) + ")";
        try (Connection c = connect(); Statement s = c.createStatement()) { s.executeUpdate(sql); }
        catch (SQLException e) { System.out.println("Ошибка БД: " + e.getMessage()); }
    }

    public List<Dish> getAllDishes() {
        List<Dish> dishes = new ArrayList<>();
        try (Connection c = connect(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT * FROM dishes")) {
            while (rs.next()) dishes.add(mapDish(rs));
        } catch (SQLException e) { System.out.println("Ошибка БД: " + e.getMessage()); }
        return dishes;
    }

    public void addOrder(Order order) {
        try (Connection c = connect(); Statement s = c.createStatement()) {
            c.setAutoCommit(false);
            s.executeUpdate("INSERT INTO orders VALUES (" + order.id + ", '" + order.customerName + "', '" + order.customerPhone + "', '" + order.status + "', '" + order.total + "')");
            for (Dish dish : order.dishes) s.executeUpdate("INSERT INTO order_items VALUES (" + order.id + ", " + dish.id + ")");
            c.commit();
        } catch (SQLException e) { System.out.println("Ошибка БД: " + e.getMessage()); }
    }

    public List<Order> getAllOrders() {
        List<Order> orders = new ArrayList<>();
        try (Connection c = connect(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT * FROM orders")) {
            while (rs.next()) {
                long orderId = rs.getLong("id");
                // ОШИБКА ОПТИМИЗАЦИИ 2 N+1: для каждого заказа выполняется отдельный запрос позиций.
                List<Dish> dishes = loadDishesForOrder(orderId);
                orders.add(new Order(orderId, rs.getString("customer_name"), rs.getString("customer_phone"), dishes, rs.getString("status"), new BigDecimal(rs.getString("total"))));
            }
        } catch (SQLException e) { System.out.println("Ошибка БД: " + e.getMessage()); }
        return orders;
    }

    private List<Dish> loadDishesForOrder(long orderId) {
        List<Dish> dishes = new ArrayList<>();
        String sql = "SELECT d.* FROM dishes d JOIN order_items oi ON d.id = oi.dish_id WHERE oi.order_id = " + orderId;
        try (Connection c = connect(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) { while (rs.next()) dishes.add(mapDish(rs)); }
        catch (SQLException e) { System.out.println("Ошибка БД: " + e.getMessage()); }
        return dishes;
    }

    private Dish mapDish(ResultSet rs) throws SQLException { return new Dish(rs.getLong("id"), rs.getString("name"), rs.getString("category"), new BigDecimal(rs.getString("price")), rs.getInt("weight"), rs.getInt("calories"), rs.getString("description"), rs.getInt("available") == 1); }
    public void updateOrderStatus(long id, String status) { try (Connection c = connect(); Statement s = c.createStatement()) { s.executeUpdate("UPDATE orders SET status = '" + status + "' WHERE id = " + id); } catch (SQLException e) { System.out.println("Ошибка БД: " + e.getMessage()); } }
    public String unsafeFindByStatus(String status) { return "SELECT * FROM orders WHERE status = '" + status + "'"; }
}
