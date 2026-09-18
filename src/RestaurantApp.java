import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Интерфейс намеренно опирается на классы с ошибками из этого проекта. */
public class RestaurantApp extends Application {
    private RestaurantController controller;
    private final ListView<String> menu = new ListView<>(), cart = new ListView<>(), history = new ListView<>();
    private final List<Dish> cartDishes = new ArrayList<>();
    private final TextField name = new TextField("Анна"), phone = new TextField("+79990000000");
    private final TextField search = new TextField();
    private final ComboBox<String> category = new ComboBox<>();
    private final ComboBox<String> status = new ComboBox<>();
    private final Label total = new Label("Стоимость заказа: 0 руб."), revenue = new Label();
    private boolean refreshing;

    @Override public void start(Stage stage) {
        // ОШИБКА ОПТИМИЗАЦИИ 6: DAO и все запросы вызываются непосредственно из JavaFX Application Thread.
        RestaurantDAO dao = new RestaurantDAO(); controller = new RestaurantController(dao);
        if (controller.getAllDishes().isEmpty()) {
            controller.addDish(new Dish(1,"Том ям","Супы",new BigDecimal("420"),350,310,"Острый суп",true));
            controller.addDish(new Dish(2,"Чай","Напитки",new BigDecimal("120"),300,5,"Черный чай",true));
            controller.addDish(new Dish(3,"Чизкейк","Десерты",new BigDecimal("260"),150,420,"Десерт",true));
        }
        menu.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        status.setItems(FXCollections.observableArrayList("NEW","COOKING","READY","COMPLETED","CANCELLED")); status.setValue("NEW");
        Button add = new Button("Добавить в заказ"); add.setOnAction(e -> addToCart());
        Button remove = new Button("Удалить позицию"); remove.setOnAction(e -> { int i=cart.getSelectionModel().getSelectedIndex(); if(i>=0){cartDishes.remove(i);refreshCart();} });
        Button create = new Button("Оформить заказ"); create.setOnAction(e -> createOrder());
        Button changeStatus = new Button("Изменить статус выбранного заказа"); changeStatus.setOnAction(e -> changeSelectedOrderStatus());
        search.setPromptText("Поиск блюда"); search.textProperty().addListener((o,a,b)->refreshAll());
        category.setOnAction(e->refreshAll());
        TextField dishName=new TextField(), dishCategory=new TextField(), dishPrice=new TextField(); dishName.setPromptText("Название"); dishCategory.setPromptText("Категория"); dishPrice.setPromptText("Цена");
        Button addDish=new Button("Добавить блюдо"); addDish.setOnAction(e->{try{long id=controller.getAllDishes().stream().mapToLong(d->d.id).max().orElse(0)+1;controller.addDish(new Dish(id,dishName.getText(),dishCategory.getText(),new BigDecimal(dishPrice.getText()),250,200,"",true));refreshAll();}catch(Exception x){warn("Проверьте данные блюда");}});
        VBox left=new VBox(8,new Label("Меню"),new HBox(4,search,category),menu,add,new HBox(4,dishName,dishCategory),new HBox(4,dishPrice,addDish));
        VBox middle=new VBox(8,new Label("Клиент"),name,phone,new Label("Текущий заказ"),cart,total,remove,create);
        VBox right=new VBox(8,new Label("История заказов"),history,new HBox(6,status,changeStatus),revenue);
        BorderPane root=new BorderPane(new HBox(14,left,middle,right)); root.setPadding(new Insets(16)); root.setTop(new Label("Ресторан  Версия с учебными ошибками")); BorderPane.setMargin(root.getTop(),new Insets(0,0,12,0)); refreshAll();
        stage.setTitle("Ресторан ошибки"); stage.setScene(new Scene(root,1050,560)); stage.show();
    }
    private List<Dish> visibleDishes(){String q=search.getText()==null?"":search.getText().toLowerCase();String c=category.getValue();return controller.getAllDishes().stream().filter(d->d.name.toLowerCase().contains(q)).filter(d->c==null||c.equals("Все")||d.category.equalsIgnoreCase(c)).toList();}
    private void addToCart(){ List<Integer> selected=menu.getSelectionModel().getSelectedIndices(); if(selected.isEmpty()){warn("Выберите блюдо.");return;} List<Dish> dishes=visibleDishes(); selected.forEach(i->cartDishes.add(dishes.get(i))); refreshCart(); menu.getSelectionModel().clearSelection(); }
    private void refreshCart(){ cart.setItems(FXCollections.observableArrayList(cartDishes.stream().map(d->d.name+" | "+d.price+" руб.").toList())); BigDecimal sum=cartDishes.stream().map(d->d.price).reduce(BigDecimal.ZERO,BigDecimal::add); total.setText("Стоимость заказа: "+sum+" руб."); }
    private void createOrder(){ if(cartDishes.isEmpty()){warn("Заказ пуст.");return;} long id=controller.getAllOrders().stream().mapToLong(o->o.id).max().orElse(0)+1; Order order=controller.createOrder(id,name.getText(),phone.getText(),List.copyOf(cartDishes)); controller.changeOrderStatus(order,status.getValue()); cartDishes.clear(); refreshCart(); refreshAll(); }
    // ОШИБКА ОПТИМИЗАЦИИ 1: одни и те же данные повторно читаются из БД без кэширования.
    //ОШИБКА ОПТИМИЗАЦИИ 5 - дубль п.1 - кэш не используется и здесь
    private void refreshAll(){if(refreshing)return;refreshing=true;String selectedCategory=category.getValue();List<String> cs=new ArrayList<>(controller.getAllDishes().stream().map(d->d.category).distinct().toList());cs.add(0,"Все");category.setItems(FXCollections.observableArrayList(cs));category.setValue(selectedCategory!=null&&cs.contains(selectedCategory)?selectedCategory:"Все");menu.setItems(FXCollections.observableArrayList(visibleDishes().stream().map(d->d.name+" | "+d.category+" | "+d.price+" руб.").toList()));history.setItems(FXCollections.observableArrayList(controller.getAllOrders().stream().map(o->"№"+o.id+" | "+o.customerName+" | "+o.status+" | "+o.total+" руб.").toList()));revenue.setText("Выручка: "+controller.getDailyStats()+" руб.");refreshing=false;}
    private void changeSelectedOrderStatus(){int index=history.getSelectionModel().getSelectedIndex();List<Order> orders=controller.getAllOrders();if(index<0||index>=orders.size()){warn("Выберите заказ в истории.");return;}controller.changeOrderStatus(orders.get(index),status.getValue());refreshAll();}
    private void warn(String t){new Alert(Alert.AlertType.WARNING,t).showAndWait();}
    public static void main(String[] args){launch(args);}
}
