package application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.animation.AnimationTimer;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.application.Platform;

public class MultiplayerGameBoard extends Pane {
	private GameClient gameClient;
	private String myName;
	private Player localPlayer;
	private Map<String, Player> activePlayers = new HashMap<>();

	private Group gameWorld;
	private Group fogOverlay; // Replaces visionMask and maskedWorld
	private javafx.scene.shape.MoveTo holeStart;
	private javafx.scene.shape.ArcTo holeArc1;
	private javafx.scene.shape.ArcTo holeArc2;
	private Circle fogFade;

	private final Text timerText;
	private final Text gameOverText;
	private final Button returnButton;
	private final Text countdownText;

	private final Set<KeyCode> activeKeys = new HashSet<>();
	private AnimationTimer gameLoop;
//	private ImageView deathGif;

	private final long roundDurationNanos = 30_000_000_000L; 
	private final long cooldownNanos = 1_000_000_000L;
	private long bombLastPassedTime = 0;
	private long roundStartTime = -1; 

	private AudioClip fuseSound;
	private AudioClip explosionSound;
	private boolean fusePlayed = false;

	private final List<Rectangle> obstacles = new ArrayList<>();
	private boolean isHost;
	
	

	public MultiplayerGameBoard(Runnable onMenuReturn, GameClient client, String myName, List<String> allPlayers, boolean isHost) {
		this.gameClient = client;
		this.myName = myName;
		this.isHost = isHost;

		// --- TIMER & UI TEXT (Unchanged) ---
		timerText = new Text(300, 50, "TIME: 60");
		timerText.setFont(Font.font("Monospaced", FontWeight.BOLD, 30));
		timerText.setFill(Color.rgb(0xE8, 0xE0, 0xC0)); 
		timerText.setStroke(Color.rgb(0x08, 0x08, 0x08)); 
		timerText.setStrokeWidth(2);
		DropShadow timerShadow = new DropShadow();
		timerShadow.setOffsetY(4); timerShadow.setOffsetX(4);
		timerShadow.setColor(Color.color(0, 0, 0, 0.7)); timerShadow.setRadius(0); 
		timerText.setEffect(timerShadow);

		// --- COUNTDOWN TEXT ---
		countdownText = new Text(350, 220, "");
		countdownText.setFont(Font.font("Monospaced", FontWeight.BOLD, 80));
		countdownText.setFill(Color.rgb(0xFF, 0x45, 0x00)); // Orange-Red
		countdownText.setStroke(Color.BLACK);
		countdownText.setStrokeWidth(3);
		countdownText.setVisible(false);
		
		gameOverText = new Text(150, 180, "");
		gameOverText.setFont(Font.font("Monospaced", FontWeight.BOLD, 22));
		gameOverText.setStroke(Color.rgb(0x08, 0x08, 0x08)); gameOverText.setStrokeWidth(1); 
		DropShadow gameOverShadow = new DropShadow();
		gameOverShadow.setOffsetY(2); gameOverShadow.setOffsetX(2);
		gameOverShadow.setColor(Color.color(0, 0, 0, 0.8)); gameOverShadow.setRadius(0);
		gameOverText.setEffect(gameOverShadow);
		gameOverText.setVisible(false);

		returnButton = new Button("BACK TO MAIN MENU"); 
		returnButton.setFont(Font.font("Monospaced", FontWeight.BOLD, 18)); 
		returnButton.setStyle("-fx-background-color: transparent; -fx-text-fill: rgb(144, 140, 128); -fx-cursor: hand;");
		returnButton.setPrefWidth(300); returnButton.setLayoutX(200); returnButton.setLayoutY(200); 
		returnButton.setVisible(false); 
		DropShadow buttonShadow = new DropShadow();
		buttonShadow.setOffsetY(2); buttonShadow.setOffsetX(2);
		buttonShadow.setColor(Color.color(0, 0, 0, 0.8)); buttonShadow.setRadius(0);
		returnButton.setEffect(buttonShadow);
		returnButton.setOnMouseEntered(e -> returnButton.setStyle("-fx-background-color: transparent; -fx-text-fill: rgb(232, 224, 192); -fx-cursor: hand;"));
		returnButton.setOnMouseExited(e -> returnButton.setStyle("-fx-background-color: transparent; -fx-text-fill: rgb(144, 140, 128); -fx-cursor: hand;"));
		returnButton.setOnAction(e -> onMenuReturn.run());

		// --- ARENA ---
		Image mapImage = new Image(getClass().getResource("/screens/arena.png").toExternalForm());
		ImageView mapBackground = new ImageView(mapImage);
		mapBackground.setFitWidth(700); mapBackground.setFitHeight(400);


		gameWorld = new Group();
		gameWorld.getChildren().add(mapBackground); 

		// --- FOG TEXTURE SETUP ---
		//		Image fogImg = new Image(getClass().getResource("/screens/cloud.png").toExternalForm());
		//		ImagePattern fogTexture = new ImagePattern(fogImg);

		// --- SPAWN PLAYERS ---
		double[] baseX = { 50, 590, 590, 50 };
		double[] baseY = { 50, 290, 50, 290 };
		for (int i = 0; i < allPlayers.size(); i++) {
			String pName = allPlayers.get(i);
			boolean startsWithBomb = (i == 0); 
			int cornerIndex = i % 4;

			int cycle = i / 4;

			double offsetX = (baseX[cornerIndex] < 350) ? (cycle * 70) : -(cycle * 70);

			double startX = baseX[cornerIndex] + offsetX;
			double startY = baseY[cornerIndex]; // Y stays the same so they form a horizontal line

			Player p = new Player(startX, startY, 
					KeyCode.UP, KeyCode.DOWN, KeyCode.LEFT, KeyCode.RIGHT, startsWithBomb, pName);

			activePlayers.put(pName, p);

			if (pName.equals(myName)) {
				this.localPlayer = p;

				// --- THE PERFECT FOG OF WAR (Even-Odd Path) ---
				double radius = 80;
				javafx.scene.shape.Path fogPath = new javafx.scene.shape.Path();

				// This is the magic rule that cuts the hole out!
				fogPath.setFillRule(javafx.scene.shape.FillRule.EVEN_ODD); 
				//				fogPath.setFill(fogTexture);
				fogPath.setFill(Color.BLACK);
				fogPath.setStroke(Color.TRANSPARENT);

				// 1. Draw the outer screen bounds (700x400)
				fogPath.getElements().addAll(
						new javafx.scene.shape.MoveTo(0, 0),
						new javafx.scene.shape.LineTo(700, 0),
						new javafx.scene.shape.LineTo(700, 400),
						new javafx.scene.shape.LineTo(0, 400),
						new javafx.scene.shape.ClosePath()
						);

				// 2. Prepare the dynamic inner hole
				holeStart = new javafx.scene.shape.MoveTo();
				holeArc1 = new javafx.scene.shape.ArcTo(radius, radius, 0, 0, 0, false, false);
				holeArc2 = new javafx.scene.shape.ArcTo(radius, radius, 0, 0, 0, false, false);

				fogPath.getElements().addAll(holeStart, holeArc1, holeArc2, new javafx.scene.shape.ClosePath());

				// 3. The soft dark edge
				fogFade = new Circle(radius + 1);
				RadialGradient fadeGradient = new RadialGradient(
						0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
						new Stop(0, Color.TRANSPARENT), 
						new Stop(1, Color.BLACK) // Changed to pure, solid black
						);
				fogFade.setFill(fadeGradient);

				fogOverlay = new Group(fogPath, fogFade);
			}
		}
		// --- GENERATE RANDOM (BUT SYNCED) OBSTACLES ---
		int numberOfObstacles = 6; 
		double obstacleSize = 40; 

		// Load image
		Image crateImg = new Image(getClass().getResource("/sprites/obstacle/fence.png").toExternalForm());
		ImagePattern crateTexture = new ImagePattern(crateImg);

		// Use the synchronized roster list to generate a shared map seed!
		// This guarantees all computers generate the exact same "random" map.
		java.util.Random mapSeeder = new java.util.Random(allPlayers.hashCode());

		for (int i = 0; i < numberOfObstacles; i++) {
			boolean isSafeSpot = false;
			Rectangle newWall = null;

			while (!isSafeSpot) {
				// Use the seeded random instead of Math.random()
				double randomX = mapSeeder.nextDouble() * (700 - obstacleSize);
				double randomY = mapSeeder.nextDouble() * (400 - obstacleSize);

				// Build the physical hitbox
				newWall = new Rectangle(randomX, randomY, obstacleSize, obstacleSize);
				newWall.setFill(crateTexture); 

				// Check against ALL dynamically spawned players
				boolean overlapsPlayer = false;
				for (Player p : activePlayers.values()) {
					if (newWall.getBoundsInParent().intersects(p.getBoundsInParent())) {
						overlapsPlayer = true;
						break; // Stop checking if we already hit someone
					}
				}

				if (!overlapsPlayer) {
					isSafeSpot = true; 
				}
			}

			obstacles.add(newWall);
		}

		gameWorld.getChildren().addAll(obstacles);

		for (Player p : activePlayers.values()) {
			gameWorld.getChildren().addAll(p, p.getNameTag());
		}

		fuseSound = new AudioClip(getClass().getResource("/music/fuse_music.wav").toExternalForm());
		explosionSound = new AudioClip(getClass().getResource("/music/explosion_music.wav").toExternalForm());
		explosionSound.setVolume(1.0);
		// --- STACK THE LAYERS ---
		// 1. GameWorld (Map + Obstacles + Players)
		// 2. FogOverlay (The Cloud Stroke + Fade)
		// 3. UI Elements (Timer, Text)
		// Notice there are ZERO BlendModes or Caches used here!
		this.getChildren().addAll(gameWorld, fogOverlay, timerText, gameOverText, returnButton);
		this.getChildren().add(countdownText);
		startGame();
	}

	private void startGame() {
		gameLoop = new AnimationTimer() {
			@Override
			public void handle(long timerNow) {
				long now = System.nanoTime(); 
				if (roundStartTime == -1) roundStartTime = now;

				long elapsedNanos = now - roundStartTime;
				long timeRemaining = (roundDurationNanos - elapsedNanos) / 1_000_000_000L;

				if (timeRemaining <= 0) {
					triggerElimination(now);
					return; 
				}

				if (timeRemaining <= 5 && !fusePlayed) {
					fuseSound.play();
					fusePlayed = true;
				}

				timerText.setText("TIME: " + timeRemaining);

				if (localPlayer != null && activePlayers.containsKey(myName)) {
					localPlayer.move(activeKeys, getWidth(), getHeight(), obstacles);

					String packet = String.format("PLAYER %s %d %d %s", 
							myName, (int)localPlayer.getX(), (int)localPlayer.getY(), 
							localPlayer.getFacing().toString()
							);
					gameClient.send(packet);

					// Make the massive stroke hole follow the player!
					if (holeStart != null) {
						double cx = localPlayer.getCenterX();
						double cy = localPlayer.getCenterY();
						double r = 80;

						// Update the two halves of the vector circle
						holeStart.setX(cx + r); holeStart.setY(cy);

						holeArc1.setX(cx - r);  holeArc1.setY(cy);
						holeArc2.setX(cx + r);  holeArc2.setY(cy);

						// Update the shadow overlay
						fogFade.setCenterX(cx);
						fogFade.setCenterY(cy);
					}
				}

				checkCollision(now);
			}
		};
		gameLoop.start();
	}

	private void checkCollision(long now) {
		if (!isHost) return; 

		if (now - bombLastPassedTime > cooldownNanos) {
			Player holder = null;
			String holderName = "";

			for (Map.Entry<String, Player> entry : activePlayers.entrySet()) {
				if (entry.getValue().hasBomb()) {
					holderName = entry.getKey();
					holder = entry.getValue();
					break;
				}
			}

			if (holder != null) {
				for (Map.Entry<String, Player> entry : activePlayers.entrySet()) {
					String potentialName = entry.getKey();
					Player potentialReceiver = entry.getValue();

					if (!potentialName.equals(holderName) && 
							holder.getBoundsInParent().intersects(potentialReceiver.getBoundsInParent())) {

						bombLastPassedTime = now; 
						gameClient.send("BOMB_PASS " + potentialName);
						break;
					}
				}
			}
		}
	}

	private void triggerElimination(long now) {
		explosionSound.play();
		fuseSound.stop();
		fusePlayed = false;

		String loserName = null;
		Player loser = null;
		for (Map.Entry<String, Player> entry : activePlayers.entrySet()) {
			if (entry.getValue().hasBomb()) {
				loserName = entry.getKey();
				loser = entry.getValue();
				break;
			}
		}

		if (loser != null) {
			final Player finalLoser = loser;
			Platform.runLater(() -> {
			    timerText.setText("TIME: 0");
			    
			    String gifPath = "/sprites/die/with_bomb_front.gif";
			    switch (finalLoser.getFacing()) {
			        case LEFT:  gifPath = "/sprites/die/with_bomb_left.gif"; break;
			        case RIGHT: gifPath = "/sprites/die/with_bomb_right.gif"; break;
			        case UP:    gifPath = "/sprites/die/with_bomb_back.gif"; break;
			        case DOWN:  gifPath = "/sprites/die/with_bomb_front.gif"; break;
			    }

			    Image freshGif = new Image(getClass().getResource(gifPath).toExternalForm());
			    // Create a local final reference for the specific death sprite
			    final ImageView currentDeathSprite = new ImageView(freshGif);

			    currentDeathSprite.setX(finalLoser.getX()); 
			    currentDeathSprite.setY(finalLoser.getY());

			    finalLoser.setVisible(false);
			    finalLoser.getNameTag().setVisible(false);

			    if (finalLoser == localPlayer && fogOverlay != null) {
			        fogOverlay.setVisible(false); 
			    }

			    // Add it to the screen
			    getChildren().add(currentDeathSprite);

			    // --- CLEANUP LOGIC ---
			    // Create a pause that lasts exactly as long as your GIF (adjust millis as needed)
			    javafx.animation.PauseTransition cleanup = new javafx.animation.PauseTransition(javafx.util.Duration.millis(1500));
			    
			    cleanup.setOnFinished(event -> {
			        getChildren().remove(currentDeathSprite);
			        System.out.println("Cleaned up dead player sprite.");
			    });
			    
			    cleanup.play();
			});

			activePlayers.remove(loserName);

			if (activePlayers.size() <= 1) {
				gameLoop.stop();
				Platform.runLater(() -> {
					String winnerName = activePlayers.keySet().iterator().next();
					gameOverText.setText("VICTORY! " + winnerName + " WINS!");
					gameOverText.setFill(Color.rgb(50,255,50));

					gameOverText.setX((700 - gameOverText.getLayoutBounds().getWidth()) / 2);
					gameOverText.toFront(); returnButton.toFront();
					gameOverText.setVisible(true); returnButton.setVisible(true);
				});
			} else {
				startIntermissionCountdown();
			}
		}
	}

	public void processNetworkMessage(String msg) {
		if (msg.startsWith("PLAYER")) {
			String[] tokens = msg.split(" ");
			String senderName = tokens[1];

			if (senderName.equals(myName)) return; 

			if (activePlayers.containsKey(senderName)) {
				Player ghost = activePlayers.get(senderName);
				Platform.runLater(() -> {
					double newX = Double.parseDouble(tokens[2]);
					double newY = Double.parseDouble(tokens[3]);
					String facingStr = tokens[4]; 

					boolean isMoving = (ghost.getX() != newX || ghost.getY() != newY);

					ghost.setX(newX);
					ghost.setY(newY);
					ghost.updateNetworkAnimation(facingStr, isMoving);
				});
			}
		}
		else if (msg.startsWith("BOMB_PASS")) {
			String newOwnerName = msg.split(" ")[1];

			Platform.runLater(() -> {
				activePlayers.values().forEach(p -> p.setBomb(false));

				if (activePlayers.containsKey(newOwnerName)) {
					activePlayers.get(newOwnerName).setBomb(true);
					bombLastPassedTime = System.nanoTime(); 
					System.out.println("[NETWORK] Bomb passed to: " + newOwnerName);
				}
			});
		}
	}
	
	private void startIntermissionCountdown() {
	    Platform.runLater(() -> {
	        countdownText.setVisible(true);
	        countdownText.toFront();

	        // Sequence: 3... 2... 1... GO!
	        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
	            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(0), e -> countdownText.setText("3")),
	            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> countdownText.setText("2")),
	            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(2), e -> countdownText.setText("1")),
	            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(3), e -> {
	                countdownText.setVisible(false);
	                resetRound();
	            })
	        );
	        timeline.play();
	    });
	}

	private void resetRound() {
	    // Deterministic bomb pass to survivors
	    List<Player> survivors = new ArrayList<>(activePlayers.values());
	    survivors.sort((p1, p2) -> p1.getNameTag().getText().compareTo(p2.getNameTag().getText()));
	    
	    // Reset bomb states
	    activePlayers.values().forEach(p -> p.setBomb(false));
	    survivors.get(0).setBomb(true);

	    // Restart timer and game loop
	    roundStartTime = System.nanoTime();
	    gameLoop.start();
	}
	
	public void addKey(KeyCode code) { activeKeys.add(code); }
	public void removeKey(KeyCode code) { activeKeys.remove(code); }
	public void setClient(GameClient client) { this.gameClient = client; }
}