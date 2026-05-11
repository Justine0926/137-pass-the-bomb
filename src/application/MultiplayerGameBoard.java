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

	private final Set<KeyCode> activeKeys = new HashSet<>();
	private AnimationTimer gameLoop;
	private ImageView deathGif;

	private final long roundDurationNanos = 30_000_000_000L; 
	private final long cooldownNanos = 1_000_000_000L;
	private long bombLastPassedTime = 0;
	private long roundStartTime = -1; 

	private AudioClip fuseSound;
	private AudioClip explosionSound;
	private boolean fusePlayed = false;
	private static Image deathGifLeft;
	private static Image deathGifRight;
	private static Image deathGifUp;
	private static Image deathGifDown;

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
		int spawnOffset = 100;
		for (int i = 0; i < allPlayers.size(); i++) {
			String pName = allPlayers.get(i);
			boolean startsWithBomb = (i == 0); 

			Player p = new Player(spawnOffset, 300, 
					KeyCode.UP, KeyCode.DOWN, KeyCode.LEFT, KeyCode.RIGHT, startsWithBomb, pName);

			activePlayers.put(pName, p);
			spawnOffset += 200; 

			if (pName.equals(myName)) {
				this.localPlayer = p;

				// --- THE PERFECT FOG OF WAR (Even-Odd Path) ---
				double radius = 80;
				javafx.scene.shape.Path fogPath = new javafx.scene.shape.Path();

				// This is the magic rule that cuts the hole out!
				fogPath.setFillRule(javafx.scene.shape.FillRule.EVEN_ODD); 
				//				fogPath.setFill(fogTexture);
				fogPath.setFill(Color.rgb(15, 20, 25, 0.98));
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
				fogFade = new Circle(radius);
				RadialGradient fadeGradient = new RadialGradient(
						0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
						new Stop(0, Color.TRANSPARENT), new Stop(1, Color.rgb(0, 0, 0, 0.9)) 
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
		// We load these ONCE here, so the GPU doesn't crash trying to load them during the explosion!
		if (deathGifLeft == null) {
			deathGifLeft = new Image(getClass().getResource("/sprites/die/with_bomb_left.gif").toExternalForm());
			deathGifRight = new Image(getClass().getResource("/sprites/die/with_bomb_right.gif").toExternalForm());
			deathGifUp = new Image(getClass().getResource("/sprites/die/with_bomb_back.gif").toExternalForm());
			deathGifDown = new Image(getClass().getResource("/sprites/die/with_bomb_front.gif").toExternalForm());
		}

		// --- STACK THE LAYERS ---
		// 1. GameWorld (Map + Obstacles + Players)
		// 2. FogOverlay (The Cloud Stroke + Fade)
		// 3. UI Elements (Timer, Text)
		// Notice there are ZERO BlendModes or Caches used here!
		this.getChildren().addAll(gameWorld, fogOverlay, timerText, gameOverText, returnButton);

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
				// Use the pre-loaded GIFs!
				Image targetGif = deathGifDown; // Default to front

				switch (finalLoser.getFacing()) {
				case LEFT: targetGif = deathGifLeft; break;
				case RIGHT: targetGif = deathGifRight; break;
				case UP: targetGif = deathGifUp; break;
				case DOWN: targetGif = deathGifDown; break;
				}

				// Assign the pre-loaded image to the ImageView
				deathGif = new ImageView(targetGif);

				deathGif.setFitWidth(60); deathGif.setFitHeight(60);
				deathGif.setX(finalLoser.getX()); deathGif.setY(finalLoser.getY());

				finalLoser.setVisible(false);
				finalLoser.getNameTag().setVisible(false);

				// Turn off the fog hole if the local player dies!
				if (finalLoser == localPlayer && fogOverlay != null) {
					fogOverlay.setVisible(false); 
				}

				getChildren().add(deathGif); 
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
				// If the game continues, give the bomb to a DETERMINISTIC survivor
				List<Player> survivors = new ArrayList<>(activePlayers.values());

				// Everyone sorts the list alphabetically by name to guarantee the exact same order
				survivors.sort((p1, p2) -> p1.getNameTag().getText().compareTo(p2.getNameTag().getText()));

				// The first survivor alphabetically gets the bomb! No randomness, no desync.
				survivors.get(0).setBomb(true);

				roundStartTime = now; // Restart the timer
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
	public void addKey(KeyCode code) { activeKeys.add(code); }
	public void removeKey(KeyCode code) { activeKeys.remove(code); }
	public void setClient(GameClient client) { this.gameClient = client; }
}