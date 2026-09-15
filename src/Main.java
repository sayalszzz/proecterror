import javafx.application.Application;

public class Main {
    public static void main(String[] args) {
        // Запуск JavaFX приложения напрямую в обход метода RestaurantApp.main
        Application.launch(RestaurantApp.class, args);
    }
}