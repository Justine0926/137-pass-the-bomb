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

	private GameServer activeServer;
	private GameClient activeClient;

	@Override
	public void start(Stage primaryStage) {
		window = primaryStage;
		window.setTitle("boom tarat tarat");
		window.setResizable(false);

		// background music
		Media sound = new Media(getClass().getResource("/music/bg_music.mp3").toExternalForm());
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
		if (activeServer != null) {
			activeServer.stop();
			activeServer = null;
		}
		if (activeClient != null) {
			activeClient.disconnect();
			activeClient = null;
		}
		// create the menu and tell it what methods to run when buttons are clicked
		MainMenu menuLayout = new MainMenu(this::startGame,this::showMultiplayerMenu, () -> window.close());
		Scene menuScene = new Scene(menuLayout, WIDTH, HEIGHT);

		window.setScene(menuScene);
	}

	// --- NEW METHOD FOR MULTIPLAYER SCREEN ---
	public void showMultiplayerMenu() {
		MultiplayerMenu mpMenu = new MultiplayerMenu(
				this::showMainMenu,

				// HOST BUTTON
				(Integer maxPlayers, String hostName) -> { 
					System.out.println("Starting Server for " + maxPlayers + " players...");

					// Use the GLOBAL variable!
					activeServer = new GameServer(maxPlayers);
					activeServer.start();

					joinLobby("localhost", true, hostName); 
				},

				// JOIN BUTTON
				(String targetIp, String joinerName) -> { 
					System.out.println("Connecting to IP: " + targetIp + " as " + joinerName);
					joinLobby(targetIp, false, joinerName); 
				}
				);

		window.setScene(new Scene(mpMenu, WIDTH, HEIGHT));
	}

	public void joinLobby(String ipAddress, boolean isHost, String myName) {

		// Show waiting screen...
		Text waitingText = new Text("WAITING FOR PLAYERS...");
		waitingText.setFont(Font.font("Monospaced", FontWeight.BOLD, 40));
		waitingText.setFill(Color.rgb(232, 224, 192));

		VBox root = new VBox(20);
		root.setAlignment(Pos.CENTER);
		root.setStyle("-fx-background-color: #111;");

		Text subText = new Text("Connected as: " + myName);
		subText.setFont(Font.font("Monospaced", 18));
		subText.setFill(Color.GRAY);

		root.getChildren().addAll(waitingText, subText);

		Scene lobbyScene = new Scene(root, WIDTH, HEIGHT);
		window.setScene(lobbyScene);

		// --- USE THE GLOBAL CLIENT ---
		activeClient = new GameClient(ipAddress, myName, msg -> {
			if (msg.startsWith("START")) {
				String roster = msg.substring(6).trim();
				List<String> allPlayers = Arrays.asList(roster.split(","));

				Platform.runLater(() -> {
					// We pass the global activeClient to the board
					MultiplayerGameBoard gameLayout = new MultiplayerGameBoard(
							this::showMainMenu, activeClient, myName, allPlayers, isHost
							);

					activeClient.setOnMessageReceived(gameLayout::processNetworkMessage);

					Scene gameScene = new Scene(gameLayout, WIDTH, HEIGHT);
					gameScene.setOnKeyPressed(event -> gameLayout.addKey(event.getCode()));
					gameScene.setOnKeyReleased(event -> gameLayout.removeKey(event.getCode()));
					window.setScene(gameScene);
				});
			}
		});

		// Don't forget to start the client thread after creating it!
		activeClient.startListening(); 
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



	@Override
	public void stop() throws Exception {
		System.out.println("Window closed. Shutting down network threads...");

		// The absolute nuclear option to guarantee all rogue threads and sockets die:
		System.exit(0); 
	}

	public static void main(String[] args) {
		launch(args);
	}
}