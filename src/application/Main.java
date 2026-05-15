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

	private List<String> tempPlayerList;

	@Override
	public void start(Stage primaryStage) {
		window = primaryStage;
		window.setTitle("boom tarat tarat");
		window.setResizable(false);

		// background music (Merged: updated to .wav from the settings branch)
		Media sound = new Media(getClass().getResource("/music/bg_music.mp3").toExternalForm());
		bgMusic = new MediaPlayer(sound);
		bgMusic.setCycleCount(MediaPlayer.INDEFINITE); // loop forever
		bgMusic.setVolume(0.5); // volume 0.0 to 1.0
		bgMusic.play();

		// Merged: Listen for settings changes to mute/unmute music
		GameSettings.onSettingsUpdated = () -> {
			bgMusic.setMute(!GameSettings.musicEnabled);
		};

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
		MainMenu menuLayout = new MainMenu(this::startGame, this::showMultiplayerMenu, () -> window.close());
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

					// --- THE FIX: Pass the Host's GameSettings into the Server! ---
					activeServer = new GameServer(maxPlayers, GameSettings.roundDurationSeconds, GameSettings.powerUpsEnabled);
					activeServer.start();

					String cleanName = hostName.trim().replaceAll(" ", "_");
					String safeHostName = cleanName + "#" + (int)(Math.random() * 9000 + 1000);
					joinLobby("localhost", true, safeHostName); 
				},

				// JOIN BUTTON
				(String targetIp, String joinerName) -> { 
					System.out.println("Connecting to IP: " + targetIp + " as " + joinerName);

					// FIX: Turn spaces into underscores!
					String cleanName = joinerName.trim().replaceAll(" ", "_");
					String safeJoinerName = cleanName + "#" + (int)(Math.random() * 9000 + 1000);
					joinLobby(targetIp, false, safeJoinerName); 
				}
				);

		window.setScene(new Scene(mpMenu, WIDTH, HEIGHT));
	}

	public void joinLobby(String ipAddress, boolean isHost, String myName) {
		VBox root = new VBox(15);
		root.setAlignment(Pos.CENTER);
		root.setStyle("-fx-background-color: #1a1a1a; -fx-padding: 20;");

		// 1. IP / JOIN CODE DISPLAY
		String localIp = "127.0.0.1";
		try { localIp = java.net.InetAddress.getLocalHost().getHostAddress(); } catch (Exception e) {}
		Text ipText = new Text("SERVER IP: " + (isHost ? localIp : ipAddress));
		ipText.setFont(Font.font("Monospaced", FontWeight.BOLD, 22));
		ipText.setFill(Color.LIGHTBLUE);

		// 2. DYNAMIC PLAYER COUNTER
		Text counterText = new Text("PLAYERS: 1 / ?");
		counterText.setFont(Font.font("Monospaced", 18));
		counterText.setFill(Color.WHITE);

		Text settingsText = new Text("DURATION: ?s  |  POWER-UPS: ?");
		settingsText.setFont(Font.font("Monospaced", 14));
		settingsText.setFill(Color.rgb(150, 150, 150));

		// 3. CHAT FEATURE
		VBox chatBox = new VBox(5);
		chatBox.setStyle("-fx-background-color: #000; -fx-padding: 10; -fx-border-color: #444; -fx-border-radius: 5;");
		chatBox.setPrefHeight(180);
		chatBox.setMaxWidth(450);

		Text chatArea = new Text("--- LOBBY CHAT ---\n");
		chatArea.setFill(Color.LIGHTGREEN);
		chatArea.setFont(Font.font("Monospaced", 12));

		// Wrap the text in a ScrollPane just in case you chat a lot!
		javafx.scene.control.ScrollPane chatScroll = new javafx.scene.control.ScrollPane(chatArea);
		chatScroll.setStyle("-fx-background: #000; -fx-border-color: transparent;");
		chatScroll.setFitToWidth(true);
		chatScroll.setPrefHeight(150);
		chatBox.getChildren().add(chatScroll);

		javafx.scene.control.TextField chatInput = new javafx.scene.control.TextField();
		chatInput.setPromptText("Press Enter to send message...");
		chatInput.setMaxWidth(450);
		chatInput.setOnAction(e -> {
			if (!chatInput.getText().trim().isEmpty()) {
				activeClient.send("LOBBY_CHAT " + myName + ": " + chatInput.getText());
				chatInput.clear();
			}
		});

		// 4. START BUTTON (Host Only)
		javafx.scene.control.Button startBtn = new javafx.scene.control.Button("START GAME");
		startBtn.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;");
		startBtn.setVisible(isHost);
		startBtn.setDisable(true); 

		// include host settings in the packet
		startBtn.setOnAction(e -> activeClient.send("FORCE_START " + GameSettings.roundDurationSeconds + " " + GameSettings.powerUpsEnabled));

		// 5. GO BACK / CANCEL BUTTON
		javafx.scene.control.Button backBtn = new javafx.scene.control.Button("CANCEL & GO BACK");
		backBtn.setStyle("-fx-background-color: #c62828; -fx-text-fill: white; -fx-font-weight: bold;");
		backBtn.setOnAction(e -> {
			// Destroy the connections so the port is freed
			if (activeServer != null) { activeServer.stop(); activeServer = null; }
			if (activeClient != null) {
				activeClient.send("DISCONNECT " + myName);
				activeClient.disconnect(); 
				activeClient = null; 
			}
			showMainMenu(); 
		});

		root.getChildren().addAll(ipText, counterText,settingsText, chatBox, chatInput, startBtn, backBtn);
		window.setScene(new Scene(root, WIDTH, HEIGHT));

		// --- THE NETWORK CLIENT LOGIC ---
		activeClient = new GameClient(ipAddress, myName, msg -> {

			// Handle Chat
			if (msg.startsWith("LOBBY_CHAT")) {
				String chatLine = msg.substring(11);
				Platform.runLater(() -> {
					chatArea.setText(chatArea.getText() + chatLine + "\n");
					chatScroll.setVvalue(1.0); // Auto-scroll to bottom
				});
			}

			// Handle Dynamic Player Joins & Settings Sync
			else if (msg.startsWith("LOBBY_UPDATE")) {
				// Split into exactly 5 pieces: [LOBBY_UPDATE, maxPlayers, duration, powerUps, roster]
				String[] parts = msg.split(" ", 5);
				String maxExpected = parts[1];
				String hostDur = parts[2];
				boolean hostPwr = Boolean.parseBoolean(parts[3]);
				String roster = parts[4].trim();

				List<String> allPlayers = Arrays.asList(roster.split(","));

				Platform.runLater(() -> {
					// Update Player Count
					counterText.setText("PLAYERS: " + allPlayers.size() + " / " + maxExpected);

					// --- NEW: Update Settings Text! ---
					settingsText.setText("DURATION: " + hostDur + "s  |  POWER-UPS: " + (hostPwr ? "ON" : "OFF"));

					this.tempPlayerList = allPlayers; 

					// Unlock the Start button for the Host if the room is full!
					if (isHost && allPlayers.size() == Integer.parseInt(maxExpected)) {
						startBtn.setDisable(false);
					}
				});
			}

			// Handle the Host clicking "Start Game"			// Handle the Host clicking "Start Game"
			else if (msg.startsWith("FORCE_START")) {
				// --- Extract BOTH of the Host's custom settings! ---
				String[] tokens = msg.split(" ");
				int hostDuration = Integer.parseInt(tokens[1]);
				boolean hostPowerUps = Boolean.parseBoolean(tokens[2]);

				Platform.runLater(() -> {
					// Pass hostPowerUps into the constructor
					MultiplayerGameBoard gameLayout = new MultiplayerGameBoard(
							this::showMainMenu, activeClient, myName, this.tempPlayerList, isHost, hostDuration, hostPowerUps
							);
					activeClient.setOnMessageReceived(gameLayout::processNetworkMessage);

					Scene gameScene = new Scene(gameLayout, WIDTH, HEIGHT);
					gameScene.setOnKeyPressed(ev -> gameLayout.addKey(ev.getCode()));
					gameScene.setOnKeyReleased(ev -> gameLayout.removeKey(ev.getCode()));
					window.setScene(gameScene);
				});
			}
		});

		activeClient.startListening();
		activeClient.send("CONNECT " + myName);
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