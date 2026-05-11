package application;
	
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

public class Main extends Application {
    
    private Stage window; // Store the main window so we can change its scenes
    private final int WIDTH = 1400;
    private final int HEIGHT = 800;

    private MediaPlayer bgMusic; // added for background music

    @Override
    public void start(Stage primaryStage) {
        window = primaryStage;
        window.setTitle("boom tarat tarat");
        window.setResizable(false);

        // background music
        Media sound = new Media(getClass().getResource("/music/bg_music.wav").toExternalForm());
        bgMusic = new MediaPlayer(sound);
        bgMusic.setCycleCount(MediaPlayer.INDEFINITE); // loop forever
        bgMusic.setVolume(0.5); // volume 0.0 to 1.0
        bgMusic.play();

        // Listen for settings changes
        GameSettings.onSettingsUpdated = () -> {
            bgMusic.setMute(!GameSettings.musicEnabled);
        };
        
        // show the main menu when the app first opens
        showMainMenu();
        
        window.show();
    }

    // method to create and display the Menu Scene
    public void showMainMenu() {
        // create the menu and tell it what methods to run when buttons are clicked
        MainMenu menuLayout = new MainMenu(this::startGame, () -> window.close());
        Scene menuScene = new Scene(menuLayout, WIDTH, HEIGHT);
        
        window.setScene(menuScene);
    }

    // method to create and display the Game Scene
    public void startGame() {
        // pass the showMainMenu method into the GameBoard!
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