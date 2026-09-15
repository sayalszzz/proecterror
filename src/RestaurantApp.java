import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RestaurantApp extends Application {
    private RestaurantController controller;

    // Оптимизация: Пул потоков для выполнения всех обращений к БД в фоне
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Оптимизация: Локальный кэш, чтобы не мучить базу данных поиском на каждую букву
    private List<Dish> cachedDishes = new ArrayList<>();
    private List<Order> cachedOrders = new ArrayList<>();

    private final ListView<String> menu = new ListView<>(), cart = new ListView<>(), history = new ListView<>();
    private final List<Dish> cartDishes = new ArrayList<>();
    private final TextField name = new TextField("Анна"), phone = new TextField("+79990000000");
    private final TextField search = new TextField();
    private final ComboBox<String> category = new ComboBox<>();
    private final ComboBox<String> status = new ComboBox<>();
    private final Label total = new Label("Стоимость заказа: 0 руб."), revenue = new Label("Выручка: Вычисляется...");
    private final ProgressIndicator progressIndicator = new ProgressIndicator();
    private boolean refreshing;

    @Override
    public void start(Stage stage) {
        RestaurantDAO dao = new RestaurantDAO();
        controller = new RestaurantController(dao);

        progressIndicator.setMaxSize(24, 24);
        progressIndicator.setVisible(false);

        // Асинхронное заполнение базы дефолтными блюдами при первом старте
        CompletableFuture.runAsync(() -> {
            if (controller.getAllDishes().isEmpty()) {
                controller.addDish(new Dish.Builder().id(1L).name("Том ям").category("Супы").price(new BigDecimal("420")).nutritionalInfo(350, 310).description("Острый суп").available(true).build());
                controller.addDish(new Dish.Builder().id(2L).name("Чай").category("Напитки").price(new BigDecimal("120")).nutritionalInfo(300, 5).description("Черный чай").available(true).build());
                controller.addDish(new Dish.Builder().id(3L).name("Чизкейк").category("Десерты").price(new BigDecimal("260")).nutritionalInfo(150, 420).description("Десерт").available(true).build());
            }
        }, executor).thenRun(this::refreshAll);

        menu.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        status.setItems(FXCollections.observableArrayList("NEW","COOKING","READY","COMPLETED","CANCELLED"));
        status.setValue("NEW");

        Button add = new Button("Добавить в заказ");
        add.setOnAction(e -> addToCart());

        Button remove = new Button("Удалить позицию");
        remove.setOnAction(e -> {
            int i = cart.getSelectionModel().getSelectedIndex();
            if (i >= 0) { cartDishes.remove(i); refreshCart(); }
        });

        Button create = new Button("Оформить заказ");
        create.setOnAction(e -> createOrder());

        Button changeStatus = new Button("Изменить статус");
        changeStatus.setOnAction(e -> changeSelectedOrderStatus());

        // Поиск мгновенно фильтрует кэш в оперативной памяти без лагов и запросов к MySQL
        search.setPromptText("Поиск блюда");
        search.textProperty().addListener((o, a, b) -> updateUiFromCache());
        category.setOnAction(e -> updateUiFromCache());

        TextField dishName = new TextField(), dishCategory = new TextField(), dishPrice = new TextField();
        dishName.setPromptText("Название"); dishCategory.setPromptText("Категория"); dishPrice.setPromptText("Цена");

        Button addDish = new Button("Добавить блюдо");
        addDish.setOnAction(e -> handleAddDish(dishName, dishCategory, dishPrice));

        VBox left = new VBox(8, new Label("Меню"), new HBox(4, search, category), menu, add, new HBox(4, dishName, dishCategory), new HBox(4, dishPrice, addDish));
        VBox middle = new VBox(8, new Label("Клиент"), name, phone, new Label("Текущий заказ"), cart, total, remove, create);
        VBox right = new VBox(8, new Label("История заказов"), history, new HBox(6, status, changeStatus), revenue);

        HBox topBar = new HBox(12, new Label("Ресторан — Версия без архитектурных ошибок"), progressIndicator);
        BorderPane root = new BorderPane(new HBox(14, left, middle, right));
        root.setPadding(new Insets(16));
        root.setTop(topBar);
        BorderPane.setMargin(root.getTop(), new Insets(0, 0, 12, 0));

        stage.setTitle("Ресторан Оптимизированный");
        stage.setScene(new Scene(root, 1050, 580));
        stage.show();
    }
    private List<Dish> visibleDishes() {
        String q = search.getText() == null ? "" : search.getText().toLowerCase().strip();
        String c = category.getValue();
        return cachedDishes.stream()
                .filter(d -> d.getName().toLowerCase().contains(q))
                .filter(d -> c == null || c.equals("Все") || d.getCategory().equalsIgnoreCase(c))
                .toList();
    }

    private void addToCart() {
        List<Integer> selected = menu.getSelectionModel().getSelectedIndices();
        if (selected.isEmpty()) { warn("Выберите блюдо."); return; }
        List<Dish> dishes = visibleDishes();
        selected.forEach(i -> cartDishes.add(dishes.get(i)));
        refreshCart();
        menu.getSelectionModel().clearSelection();
    }

    private void refreshCart() {
        cart.setItems(FXCollections.observableArrayList(cartDishes.stream().map(d -> d.getName() + " | " + d.getPrice() + " руб.").toList()));
        BigDecimal sum = cartDishes.stream().map(Dish::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        total.setText("Стоимость заказа: " + sum + " руб.");
    }

    // Быстрая отрисовка списков на основе локальной памяти (кэша)
    private void updateUiFromCache() {
        menu.setItems(FXCollections.observableArrayList(
                visibleDishes().stream().map(d -> d.getName() + " | " + d.getCategory() + " | " + d.getPrice() + " руб.").toList()
        ));
        history.setItems(FXCollections.observableArrayList(
                cachedOrders.stream().map(o -> "№" + o.getId() + " | " + o.getCustomer().getName() + " | " + o.getStatus() + " | " + o.getTotal() + " руб.").toList()
        ));
    }

    // Загрузка всех данных из MySQL в фоне ОДИН раз за цикл обновления
    @SuppressWarnings("unchecked")
    private void refreshAll() {
        if (refreshing) return;
        refreshing = true;
        progressIndicator.setVisible(true);

        CompletableFuture.supplyAsync(() -> {
            List<Dish> dishes = controller.getAllDishes();
            List<Order> orders = controller.getAllOrders();
            BigDecimal stats = controller.getDailyStats();
            return new Object[]{dishes, orders, stats};
        }, executor).thenAccept(data -> Platform.runLater(() -> {
            Object[] dataArray = (Object[]) data;
            cachedDishes = (List<Dish>) dataArray[0];
            cachedOrders = (List<Order>) dataArray[1];
            BigDecimal dailyRevenue = (BigDecimal) dataArray[2];

            String selectedCategory = category.getValue();
            List<String> cs = new ArrayList<>(cachedDishes.stream().map(Dish::getCategory).distinct().toList());
            cs.add(0, "Все");
            category.setItems(FXCollections.observableArrayList(cs));
            category.setValue(selectedCategory != null && cs.contains(selectedCategory) ? selectedCategory : "Все");

            updateUiFromCache();
            revenue.setText("Выручка: " + dailyRevenue + " руб.");

            refreshing = false;
            progressIndicator.setVisible(false);
        }));
    }
    // Асинхронное создание заказа через паттерны Builder и Value Object
    private void createOrder() {
        if (cartDishes.isEmpty()) { warn("Заказ пуст."); return; }
        String customerName = name.getText();
        String customerPhone = phone.getText();
        String initialStatus = status.getValue();
        List<Dish> dishesToOrder = List.copyOf(cartDishes);

        progressIndicator.setVisible(true);
        CompletableFuture.runAsync(() -> {
            long nextId = cachedOrders.stream().mapToLong(Order::getId).max().orElse(0) + 1;

            Order.Builder orderBuilder = new Order.Builder()
                    .id(nextId)
                    .customer(customerName, customerPhone)
                    .status(initialStatus);
            dishesToOrder.forEach(orderBuilder::dish);

            controller.createOrder(orderBuilder.build());
        }, executor).thenRun(() -> Platform.runLater(() -> {
            cartDishes.clear();
            refreshCart();
            refreshAll();
        })).exceptionally(ex -> {
            Platform.runLater(() -> warn("Ошибка оформления: " + ex.getCause().getMessage()));
            return null;
        });
    }

    // Асинхронное изменение статуса выбранного в таблице заказа
    private void changeSelectedOrderStatus() {
        int index = history.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= cachedOrders.size()) { warn("Выберите заказ в истории."); return; }

        Order selectedOrder = cachedOrders.get(index);
        String newStatus = status.getValue();

        progressIndicator.setVisible(true);
        CompletableFuture.runAsync(() -> {
            controller.changeOrderStatus(selectedOrder, newStatus);
        }, executor).thenRun(this::refreshAll);
    }

    // Асинхронное добавление нового блюда в меню через админку снизу
    private void handleAddDish(TextField dishName, TextField dishCategory, TextField dishPrice) {
        try {
            String nameText = dishName.getText();
            String catText = dishCategory.getText();
            BigDecimal priceVal = new BigDecimal(dishPrice.getText());

            progressIndicator.setVisible(true);
            CompletableFuture.runAsync(() -> {
                long nextId = controller.getAllDishes().stream().mapToLong(Dish::getId).max().orElse(0) + 1;

                Dish newDish = new Dish.Builder()
                        .id(nextId)
                        .name(nameText)
                        .category(catText)
                        .price(priceVal)
                        .nutritionalInfo(250, 200)
                        .description("")
                        .available(true)
                        .build();

                controller.addDish(newDish);
            }, executor).thenRun(() -> Platform.runLater(() -> {
                dishName.clear(); dishCategory.clear(); dishPrice.clear();
                refreshAll();
            })).exceptionally(ex -> {
                Platform.runLater(() -> warn("Ошибка валидации: " + ex.getCause().getMessage()));
                return null;
            });
        } catch (Exception x) {
            warn("Проверьте корректность цены.");
            progressIndicator.setVisible(false);
        }
    }

    private void warn(String t) {
        new Alert(Alert.AlertType.WARNING, t).showAndWait();
    }

    @Override
    public void stop() {
        // Безопасная остановка фонового пула при закрытии крестиком окна приложения
        executor.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
} // Самая последняя закрывающая скобка класса RestaurantApp
