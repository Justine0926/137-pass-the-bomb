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
	//	time sync var
	private long lastBroadcastedTime = -1;

	private final Set<KeyCode> activeKeys = new HashSet<>();
	private AnimationTimer gameLoop;
	//	private ImageView deathGif;

	//	private final long roundDurationNanos = 5_000_000_000L; 
	private final long roundDurationNanos;
	private final long cooldownNanos = 1_000_000_000L;
	private long bombLastPassedTime = 0;
	private long roundStartTime = -1; 
	private final boolean powerUpsEnabled;

	private AudioClip fuseSound;
	private AudioClip explosionSound;
	private boolean fusePlayed = false;

	private final List<Rectangle> obstacles = new ArrayList<>();
	private boolean isHost;
	// --- POWERUPS & TRAPS ---
	private final Map<String, PowerUp> networkPowerUps = new HashMap<>();
	private long lastPowerUpSpawnTime = 0;
	private final long spawnIntervalNanos = 7_000_000_000L;
	private final int maxActivePowerUps = 2;
	private long lastTrapSpawnTime = 0;
	private final long trapSpawnIntervalNanos = 5_000_000_000L;

	private final Circle[] freezeTraps = new Circle[2];
	// Add these to the top of your class to track the Host's exact nanosecond timers
	private final Map<String, Long> hostFreezeTimers = new HashMap<>();
	private final Map<String, Long> hostSpeedTimers = new HashMap<>();



	public MultiplayerGameBoard(Runnable onMenuReturn, GameClient client, String myName, List<String> allPlayers, boolean isHost, int hostDuration, boolean hostPowerUps) {
		this.gameClient = client;
		this.myName = myName;
		this.isHost = isHost;
		this.roundDurationNanos = hostDuration * 1_000_000_000L;
		this.powerUpsEnabled = hostPowerUps;

		// --- TIMER & UI TEXT (Unchanged) ---
		timerText = new Text(300, 50, "TIME: " + hostDuration);
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
		returnButton.setOnAction(e -> {
			// shut down the client socket so it stops listening
			if (gameClient != null) {
				gameClient.disconnect(); // Make sure your GameClient has a stop() method that calls socket.close()
			}

			//return to the main menu
			onMenuReturn.run(); 
		});

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
			// Added p.getShieldAura() to the scene graph!
			gameWorld.getChildren().addAll(p, p.getShieldAura(), p.getNameTag());
		}

		for (int i = 0; i < 2; i++) {
			freezeTraps[i] = new Circle(20, Color.CYAN);
			freezeTraps[i].setStroke(Color.WHITE);
			freezeTraps[i].setStrokeWidth(3);
			freezeTraps[i].setVisible(false); // Hidden until Host spawns them
			gameWorld.getChildren().add(freezeTraps[i]);
		}

		try {
			fuseSound = new AudioClip(getClass().getResource("/music/fuse_music.mp3").toExternalForm());
			explosionSound = new AudioClip(getClass().getResource("/music/explosion_music.mp3").toExternalForm());

			// Check the player's LOCAL setting. Do not trust the network for this!
			if (!GameSettings.sfxEnabled) {
				fuseSound.setVolume(0);
				explosionSound.setVolume(0);
			} else {
				fuseSound.setVolume(1.0);
				explosionSound.setVolume(1.0);
			}
		} catch (Exception e) {
			System.err.println("Warning: Could not load multiplayer sound effects.");
		}
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

				// 1. HOST-AUTHORITATIVE TIME LOGIC
				if (isHost) {
					if (roundStartTime == -1) roundStartTime = now;

					long elapsedNanos = now - roundStartTime;
					long timeRemaining = (roundDurationNanos - elapsedNanos) / 1_000_000_000L;

					// If time is up, Host tells everyone to blow up!
					if (timeRemaining <= 0) {
						gameClient.send("ELIMINATE"); 
						triggerElimination(now);
						return; 
					}

					// Only send a network packet if the second actually changed
					if (timeRemaining != lastBroadcastedTime) {
						gameClient.send("TIME " + timeRemaining);
						lastBroadcastedTime = timeRemaining;

						// Host updates its own text and sound natively
						timerText.setText("TIME: " + timeRemaining);
						if (timeRemaining <= 5 && !fusePlayed) {
							fuseSound.play();
							fusePlayed = true;
						}
					}

					// --- NEW: HOST FOG SYNC ---
					// Dynamically calculate elapsed time so it works even if you change round duration later!
					long totalRoundSeconds = roundDurationNanos / 1_000_000_000L;
					long elapsedSeconds = totalRoundSeconds - timeRemaining;

					// A 13-second cycle (10 seconds ON, 3 seconds OFF)
					long cycle = elapsedSeconds % 13; 
					boolean shouldFogBeOn = (cycle < 10);

					if (fogOverlay != null && activePlayers.containsKey(myName)) {
						fogOverlay.setVisible(shouldFogBeOn);
					}
				}

				// 2. PLAYER MOVEMENT (Both Host and Clients still do this!)
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

						holeStart.setX(cx + r); holeStart.setY(cy);
						holeArc1.setX(cx - r);  holeArc1.setY(cy);
						holeArc2.setX(cx + r);  holeArc2.setY(cy);

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

		// 1. HOST REMOVES EXPIRED BUFFS
		hostFreezeTimers.entrySet().removeIf(entry -> {
			if (now >= entry.getValue()) {
				gameClient.send("REMOVE_FREEZE " + entry.getKey());
				return true; 
			}
			return false;
		});

		hostSpeedTimers.entrySet().removeIf(entry -> {
			if (now >= entry.getValue()) {
				gameClient.send("REMOVE_SPEED " + entry.getKey());
				return true;
			}
			return false;
		});

		// 2. BOMB PASSING
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
							holder.getHitbox().intersects(potentialReceiver.getHitbox())) {

						if (potentialReceiver.isShielded()) {
							potentialReceiver.breakShield(); 
							gameClient.send("BREAK_SHIELD " + potentialName); 

							// FIX: Trigger the cooldown so the bomb doesn't pass on the very next frame!
							bombLastPassedTime = now; 
							break; 
						}
						else {
							gameClient.send("BOMB_PASS " + potentialName);

							// Trigger the cooldown for a normal pass
							bombLastPassedTime = now; 
							break;
						}
					}
				}
			}
		}

		// 3. HOST SPAWNS POWERUPS (ONLY IF ALLOWED)
		if (powerUpsEnabled && now - lastPowerUpSpawnTime > spawnIntervalNanos) {

			// Try to spawn a PowerUp if we aren't at the max limit
			if (networkPowerUps.size() < maxActivePowerUps) {
				String type = Math.random() < 0.5 ? "SPEED" : "SHIELD";

				double px = Math.random() * (700 - 80) + 10;
				double py = Math.random() * (400 - 80) + 10;
				String puID = "PU_" + now; 

				gameClient.send("SPAWN_POWERUP " + puID + " " + type + " " + px + " " + py);
			}
			lastPowerUpSpawnTime = now;
		}

		// --- NEW: HOST SPAWNS TRAPS (ONLY IF ALLOWED) ---
		if (powerUpsEnabled && now - lastTrapSpawnTime > trapSpawnIntervalNanos) { 
			for (int i = 0; i < freezeTraps.length; i++) {
				if (!freezeTraps[i].isVisible()) {
					double tx = Math.random() * (660 - 40) + 20;
					double ty = Math.random() * (360 - 40) + 20;
					gameClient.send("SPAWN_TRAP " + i + " " + tx + " " + ty);
					break; 
				}
			}
			lastTrapSpawnTime = now;
		}

		// 4. HOST DETECTS ITEM PICKUPS
		for (Map.Entry<String, Player> entry : activePlayers.entrySet()) {
			String pName = entry.getKey();
			Player p = entry.getValue();

			// A. Did this player hit a Trap?
			for (int i = 0; i < freezeTraps.length; i++) {
				if (freezeTraps[i].isVisible() && p.getHitbox().intersects(freezeTraps[i].getBoundsInParent())) {
					gameClient.send("APPLY_FREEZE " + i + " " + pName);
					freezeTraps[i].setVisible(false); 

					// Record the exact nano-time it should end (5 seconds from now)
					hostFreezeTimers.put(pName, now + 5_000_000_000L);
				}
			}

			// B. Did this player hit a PowerUp?
			for (Map.Entry<String, PowerUp> puEntry : networkPowerUps.entrySet()) {
				PowerUp pu = puEntry.getValue();

				// Check intersection
				if (!pu.isCollected() && p.getHitbox().intersects(pu.getBounds())) {

					// Tell all clients to apply the effect and delete the visual sprite
					gameClient.send("APPLY_POWERUP " + puEntry.getKey() + " " + pu.getType().toString() + " " + pName);
					pu.collect(); // Mark it collected so the Host doesn't spam this packet 60 times a second

					// If it's a speed boost, the Host MUST track the 4-second timer
					if (pu.getType() == PowerUp.PowerUpType.SPEED) {
						hostSpeedTimers.put(pName, now + 4_000_000_000L);
					}
				}
			}
		}
	}

	private void triggerElimination(long now) {
		// 1. STOP THE LOOP IMMEDIATELY! 
		// This freezes movement and prevents the Host from spamming "ELIMINATE"
		gameLoop.stop(); 

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
				final ImageView currentDeathSprite = new ImageView(freshGif);

				currentDeathSprite.setX(finalLoser.getX()); 
				currentDeathSprite.setY(finalLoser.getY());

				finalLoser.setVisible(false);
				finalLoser.getNameTag().setVisible(false);
				finalLoser.setShielded(false);

				if (finalLoser == localPlayer && fogOverlay != null) {
					fogOverlay.setVisible(false); 
				}

				getChildren().add(currentDeathSprite);

				javafx.animation.PauseTransition cleanup = new javafx.animation.PauseTransition(javafx.util.Duration.millis(1500));

				cleanup.setOnFinished(event -> {
					getChildren().remove(currentDeathSprite);
					System.out.println("Cleaned up dead player sprite.");
				});

				cleanup.play();
			});

			activePlayers.remove(loserName);

			// 2. CLEAN UP LINGERING EFFECTS FOR SURVIVORS
			activePlayers.values().forEach(p -> {
				p.resetSpeed();
				p.setFrozen(false);
				p.setShielded(false);
			});
			if (isHost) {
				hostFreezeTimers.clear();
				hostSpeedTimers.clear();
			}

			if (activePlayers.size() <= 1) {
				// gameLoop.stop() was removed from here because we moved it to the top!
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
		//		time sync
		else if (msg.startsWith("TIME")) {
			if (isHost) return; 

			// Added .trim() to destroy invisible UDP garbage data!
			long networkTime = Long.parseLong(msg.split(" ")[1].trim());

			Platform.runLater(() -> {
				timerText.setText("TIME: " + networkTime);

				if (networkTime <= 5 && !fusePlayed && networkTime > 0) {
					fuseSound.play();
					fusePlayed = true;
				}
				// --- NEW: CLIENT FOG SYNC ---
				long totalRoundSeconds = roundDurationNanos / 1_000_000_000L;
				long elapsedSeconds = totalRoundSeconds - networkTime;

				// A 13-second cycle (10 seconds ON, 3 seconds OFF)
				long cycle = elapsedSeconds % 13; 
				boolean shouldFogBeOn = (cycle < 10);

				if (fogOverlay != null && activePlayers.containsKey(myName)) {
					fogOverlay.setVisible(shouldFogBeOn);
				}
			});
		}

		// announced elimination
		else if (msg.equals("ELIMINATE")) {
			if (isHost) return; 

			// If the Host says time is up, immediately trigger the explosion!
			Platform.runLater(() -> triggerElimination(System.nanoTime()));
		}
		// --- ITEM SPAWNING ---
		else if (msg.startsWith("SPAWN_POWERUP")) {
			String[] tokens = msg.split(" ");
			String puID = tokens[1];
			PowerUp.PowerUpType type = PowerUp.PowerUpType.valueOf(tokens[2]);
			double px = Double.parseDouble(tokens[3]);
			double py = Double.parseDouble(tokens[4]);

			Platform.runLater(() -> {
				PowerUp pu = new PowerUp(type, px, py);
				networkPowerUps.put(puID, pu);
				gameWorld.getChildren().add(pu.getSprite());
			});
		}
		else if (msg.startsWith("SPAWN_TRAP")) {
			String[] tokens = msg.split(" ");
			int trapIndex = Integer.parseInt(tokens[1]);
			double tx = Double.parseDouble(tokens[2]);
			double ty = Double.parseDouble(tokens[3]);

			Platform.runLater(() -> {
				freezeTraps[trapIndex].setCenterX(tx);
				freezeTraps[trapIndex].setCenterY(ty);
				freezeTraps[trapIndex].setVisible(true);
			});
		}

		// --- APPLYING EFFECTS ---
		else if (msg.startsWith("APPLY_POWERUP")) {
			String[] tokens = msg.split(" ");
			String puID = tokens[1];
			String typeStr = tokens[2];
			String targetPlayerName = tokens[3];

			Platform.runLater(() -> {
				// Remove visual powerup from the board
				if (networkPowerUps.containsKey(puID)) {
					PowerUp pu = networkPowerUps.remove(puID);
					gameWorld.getChildren().remove(pu.getSprite());
				}

				// Apply the buff
				if (activePlayers.containsKey(targetPlayerName)) {
					Player target = activePlayers.get(targetPlayerName);

					if (typeStr.equals("SPEED")) {
						// STRICT HOST-AUTHORITY: Turn it on forever (until the Host says stop!)
						target.applySpeedBoost(1.8); 
					} else if (typeStr.equals("SHIELD")) {
						target.setShielded(true);
					}
				}
			});
		}

		// --- NEW: LISTEN FOR HOST REMOVING SPEED ---
		else if (msg.startsWith("REMOVE_SPEED")) {
			String targetPlayerName = msg.split(" ")[1];

			Platform.runLater(() -> {
				if (activePlayers.containsKey(targetPlayerName)) {
					// The Host said time is up! Remove the speed boost.
					activePlayers.get(targetPlayerName).resetSpeed();
				}
			});
		}
		// --- STRICT SERVER-AUTHORITATIVE EFFECTS ---
		else if (msg.startsWith("APPLY_FREEZE")) {
			String[] tokens = msg.split(" ");
			int trapIndex = Integer.parseInt(tokens[1]);
			String targetPlayerName = tokens[2];

			Platform.runLater(() -> {
				freezeTraps[trapIndex].setVisible(false); 
				if (activePlayers.containsKey(targetPlayerName)) {
					// Turn it ON and leave it on forever (until the Host says otherwise)
					activePlayers.get(targetPlayerName).setFrozen(true);
				}
			});
		}
		else if (msg.startsWith("REMOVE_FREEZE")) {
			String targetPlayerName = msg.split(" ")[1];

			Platform.runLater(() -> {
				if (activePlayers.containsKey(targetPlayerName)) {
					// The Host said time is up! Unfreeze them.
					activePlayers.get(targetPlayerName).setFrozen(false);
				}
			});
		}
		else if (msg.startsWith("BREAK_SHIELD")) {
			String targetPlayerName = msg.split(" ")[1];
			Platform.runLater(() -> {
				if (activePlayers.containsKey(targetPlayerName)) {
					activePlayers.get(targetPlayerName).breakShield();
				}
			});
		}
		else if (msg.startsWith("INTERMISSION")) {
			String countNumber = msg.split(" ")[1];

			Platform.runLater(() -> {
				countdownText.setText(countNumber);
				countdownText.setVisible(true);
				countdownText.toFront();
			});
		}
		else if (msg.equals("RESTART_ROUND")) {
			Platform.runLater(() -> {
				countdownText.setVisible(false);
				resetRound(); // EVERYONE restarts the exact millisecond the Host says so!
			});
		}
	}

	private void startIntermissionCountdown() {
		// STRICT HOST-AUTHORITY: Only the Host runs the 3-second timer!
		if (!isHost) return;

		Platform.runLater(() -> {
			// Sequence: 3... 2... 1... GO!
			javafx.animation.Timeline timeline = new javafx.animation.Timeline(
					new javafx.animation.KeyFrame(javafx.util.Duration.seconds(0), e -> gameClient.send("INTERMISSION 3")),
					new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> gameClient.send("INTERMISSION 2")),
					new javafx.animation.KeyFrame(javafx.util.Duration.seconds(2), e -> gameClient.send("INTERMISSION 1")),
					new javafx.animation.KeyFrame(javafx.util.Duration.seconds(3), e -> gameClient.send("RESTART_ROUND"))
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

		// 3. RESET HOST ITEM TIMERS
		// Prevents traps and powerups from instantly spawning because 3 seconds passed in real-life
		if (isHost) {
			lastPowerUpSpawnTime = roundStartTime;
			lastTrapSpawnTime = roundStartTime;
			bombLastPassedTime = roundStartTime;
		}

		gameLoop.start();
	}

	public void addKey(KeyCode code) { activeKeys.add(code); }
	public void removeKey(KeyCode code) { activeKeys.remove(code); }
	public void setClient(GameClient client) { this.gameClient = client; }
}