package application;
	
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    
    private Stage window; // Store the main window so we can change its scenes
    private final int WIDTH = 800;
    private final int HEIGHT = 600;

    @Override
    public void start(Stage primaryStage) {
        window = primaryStage;
        window.setTitle("137 Pass The Bomb");
        window.setResizable(false);
        
        // Show the main menu when the app first opens
        showMainMenu();
        
        window.show();
    }

    // Method to create and display the Menu Scene
    public void showMainMenu() {
        // Create the menu and tell it what methods to run when buttons are clicked
        MainMenu menuLayout = new MainMenu(this::startGame, () -> window.close());
        Scene menuScene = new Scene(menuLayout, WIDTH, HEIGHT);
        
        window.setScene(menuScene);
    }

    // Method to create and display the Game Scene
    public void startGame() {
        // Pass the showMainMenu method into the GameBoard!
        GameBoard gameLayout = new GameBoard(this::showMainMenu);
        
        Scene gameScene = new Scene(gameLayout, WIDTH, HEIGHT);
        
        gameScene.setOnKeyPressed(event -> gameLayout.addKey(event.getCode()));
        gameScene.setOnKeyReleased(event -> gameLayout.removeKey(event.getCode()));

        window.setScene(gameScene);
    }
	
    public static void main(String[] args) {
        launch(args);
    }
}