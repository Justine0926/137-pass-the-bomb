package application;

import java.util.Arrays;
import java.util.List;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class Main extends Application {

	private Stage window; // Store the main window so we can change its scenes
	private final int WIDTH = 700;
	private final int HEIGHT = 400;

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

		// show the main menu when the app first opens
		showMainMenu();

		window.show();
	}

	// method to create and display the Menu Scene
	public void showMainMenu() {
		// create the menu and tell it what methods to run when buttons are clicked
		MainMenu menuLayout = new MainMenu(this::startGame,this::showMultiplayerMenu, () -> window.close());
		Scene menuScene = new Scene(menuLayout, WIDTH, HEIGHT);

		window.setScene(menuScene);
	}

	// --- NEW METHOD FOR MULTIPLAYER SCREEN ---
	public void showMultiplayerMenu() {
		MultiplayerMenu mpMenu = new MultiplayerMenu(
				this::showMainMenu,

				// HOST BUTTON (Now provides the Player Count and the Host's Name)
				(Integer maxPlayers, String hostName) -> { 
					System.out.println("Starting Server for " + maxPlayers + " players...");

					// Pass the requested size into the server!
					GameServer localServer = new GameServer(maxPlayers);
					localServer.start();

					// Join the lobby with our custom name!
					joinLobby("localhost", true, hostName); 
				},

				// JOIN BUTTON (Now provides the IP and the Joiner's Name)
				(String targetIp, String joinerName) -> { 
					System.out.println("Connecting to IP: " + targetIp + " as " + joinerName);

					// Join the lobby with our custom name!
					joinLobby(targetIp, false, joinerName); 
				}
				);

		window.setScene(new Scene(mpMenu, WIDTH, HEIGHT));
	}

	public void joinLobby(String ipAddress, boolean isHost, String myName) {
	    // 1. Declare the variable at the top level of the method
	    // We use an array trick or a class field if we need to access it inside the lambda
	    final GameClient[] clientWrapper = new GameClient[1]; 

	    // 2. Show waiting screen...
	    Text waitingText = new Text("WAITING FOR PLAYERS...");
	    waitingText.setFont(Font.font("Monospaced", FontWeight.BOLD, 40));
	    waitingText.setFill(Color.rgb(232, 224, 192));
	    
	    VBox root = new VBox(20);
	    root.setAlignment(Pos.CENTER);
	    root.setStyle("-fx-background-color: #111;");
	    
	    // Let's add a little detail so you know it's working
	    Text subText = new Text("Connected as: " + myName);
	    subText.setFont(Font.font("Monospaced", 18));
	    subText.setFill(Color.GRAY);
	    
	    root.getChildren().addAll(waitingText, subText);
	    
	    Scene lobbyScene = new Scene(root, WIDTH, HEIGHT);
	    window.setScene(lobbyScene);
	    // 3. Initialize the client
	    clientWrapper[0] = new GameClient(ipAddress, myName, msg -> {
	        if (msg.startsWith("START")) {
	            String roster = msg.substring(6).trim();
	            List<String> allPlayers = Arrays.asList(roster.split(","));

	            Platform.runLater(() -> {
	                MultiplayerGameBoard gameLayout = new MultiplayerGameBoard(
	                    this::showMainMenu, clientWrapper[0], myName, allPlayers, isHost
	                );
	                
	                // Now this is safe because clientWrapper[0] is definitely assigned
	                clientWrapper[0].setOnMessageReceived(gameLayout::processNetworkMessage);
	                
	                Scene gameScene = new Scene(gameLayout, WIDTH, HEIGHT);
	            	gameScene.setOnKeyPressed(event -> gameLayout.addKey(event.getCode()));
	            	gameScene.setOnKeyReleased(event -> gameLayout.removeKey(event.getCode()));
	            	window.setScene(gameScene);
	            });
	        }
	    });
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