package application;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javafx.animation.AnimationTimer;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.effect.BlendMode;
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

// THIS IS THE CLASS FOR THE GAMEBOARD/MAP
// where the game takes place
public class GameBoard extends Pane {
	// initialize players
	private final Player player1;
	private final Player player2;
	private Group gameWorld;

	// screen layout
	private final Text timerText;
	private final Text gameOverText;
	private final Button returnButton; 

	// moves storage
	private final Set<KeyCode> activeKeys = new HashSet<>();
	private AnimationTimer gameLoop;
	private ImageView deathGif;

	// Dynamic Time from Settings!
	private final long gameDurationNanos = GameSettings.roundDurationSeconds * 1_000_000_000L;
	private final long cooldownNanos = 1_000_000_000L;
	private long bombLastPassedTime = 0;

	// limited vision variables
	private final Circle p1Vision;
	private final Circle p2Vision;

	// sound
	// TEMP: disabled until JavaFX media playback is restored on Fedora 43.
	// Flip AUDIO_ENABLED to re-enable.
	private static final boolean AUDIO_ENABLED = false;
	private AudioClip fuseSound;
	private AudioClip explosionSound;
	private boolean fusePlayed = false;

	// obstacles
	private final List<Rectangle> obstacles = new ArrayList<>();

	// randomized bomb holder
	boolean bombHolder = Math.random() < 0.5;

	// freeze (copy pasted)
	private final Circle[] freezeTraps = new Circle[2];
	private long p1FrozenUntil = 0;
	private long p2FrozenUntil = 0;

	// powerup logic
	private final List<PowerUp> activePowerUps = new ArrayList<>();
	private long lastPowerUpSpawnTime = 0;
	private final long spawnIntervalNanos = 7_000_000_000L; // ~7 seconds between spawns
	private final int  maxActivePowerUps  = 2;

	// speed boost tracking (per player)
	private long p1SpeedBoostEnd = 0;
	private long p2SpeedBoostEnd = 0;
	private final long speedBoostDurationNanos = 4_000_000_000L; // 4 seconds

	// we now pass a Runnable so the board knows how to go back to the menu
	public GameBoard(Runnable onMenuReturn) {
		player1 = new Player(200, 200, 
				KeyCode.W, KeyCode.S, KeyCode.A, KeyCode.D, bombHolder, "PLAYER 1");

		player2 = new Player(500, 200, 
				KeyCode.UP, KeyCode.DOWN, KeyCode.LEFT, KeyCode.RIGHT, !bombHolder, "PLAYER 2");

		// --- TIMER TEXT ---
		// Merged: Kept X:300 so it doesn't fall off the screen, but applied dynamic GameSettings time
		timerText = new Text(300, 50, "TIME: " + GameSettings.roundDurationSeconds);
		timerText.setFont(Font.font("Monospaced", FontWeight.BOLD, 30));
		timerText.setFill(Color.rgb(0xE8, 0xE0, 0xC0)); // MainMenu ACCENT color
		timerText.setStroke(Color.rgb(0x08, 0x08, 0x08)); // MainMenu BLACK
		timerText.setStrokeWidth(2);

		// drop shadow
		DropShadow timerShadow = new DropShadow();
		timerShadow.setOffsetY(4);
		timerShadow.setOffsetX(4);
		timerShadow.setColor(Color.color(0, 0, 0, 0.7));
		timerShadow.setRadius(0); 
		timerText.setEffect(timerShadow);

		// --- GAME OVER TEXT ---
		gameOverText = new Text(150, 180, "");
		gameOverText.setFont(Font.font("Monospaced", FontWeight.BOLD, 22));
		gameOverText.setStroke(Color.rgb(0x08, 0x08, 0x08)); 
		gameOverText.setStrokeWidth(1); 

		DropShadow gameOverShadow = new DropShadow();
		gameOverShadow.setOffsetY(6);
		gameOverShadow.setOffsetX(6);
		gameOverShadow.setColor(Color.color(0, 0, 0, 0.8));
		gameOverShadow.setRadius(0);
		gameOverText.setEffect(gameOverShadow);

		gameOverText.setVisible(false);

		// ---RETURN BUTTON ---
		returnButton = new Button("BACK TO MAIN MENU"); 
		returnButton.setFont(Font.font("Monospaced", FontWeight.BOLD, 18)); 
		returnButton.setStyle("-fx-background-color: transparent; -fx-text-fill: rgb(144, 140, 128); -fx-cursor: hand;");
		returnButton.setPrefWidth(300);
		returnButton.setLayoutX(200); 
		returnButton.setLayoutY(200); 
		returnButton.setVisible(false); 
		DropShadow buttonShadow = new DropShadow();
		buttonShadow.setOffsetY(2);
		buttonShadow.setOffsetX(2);
		buttonShadow.setColor(Color.color(0, 0, 0, 0.8));
		buttonShadow.setRadius(0);
		returnButton.setEffect(buttonShadow);

		// --- BUTTON HOVER EFFECTS ---
		returnButton.setOnMouseEntered(e -> {
			returnButton.setStyle("-fx-background-color: transparent; -fx-text-fill: rgb(232, 224, 192); -fx-cursor: hand;");
		});

		returnButton.setOnMouseExited(e -> {
			returnButton.setStyle("-fx-background-color: transparent; -fx-text-fill: rgb(144, 140, 128); -fx-cursor: hand;");
		});

		returnButton.setOnAction(e -> onMenuReturn.run());

		// --- BACKGROUND ARENA LOGIC ---
		Image mapImage = new Image(getClass().getResource("/screens/arena.png").toExternalForm());
		ImageView mapBackground = new ImageView(mapImage);
		mapBackground.setFitWidth(700);
		mapBackground.setFitHeight(400);

		// --- RANDOM OBSTACLE SETUP ---
		int numberOfObstacles = 6; 
		double obstacleSize = 40; 

		Image crateImg = new Image(getClass().getResource("/sprites/obstacle/fence.png").toExternalForm());
		ImagePattern crateTexture = new ImagePattern(crateImg);

		for (int i = 0; i < numberOfObstacles; i++) {
			boolean isSafeSpot = false;
			Rectangle newWall = null;

			while (!isSafeSpot) {
				double randomX = Math.random() * (660 - obstacleSize);
				double randomY = Math.random() * (400 - obstacleSize);

				newWall = new Rectangle(randomX, randomY, obstacleSize, obstacleSize);
				newWall.setFill(crateTexture); 

				if (!newWall.getBoundsInParent().intersects(player1.getBoundsInParent()) && 
					!newWall.getBoundsInParent().intersects(player2.getBoundsInParent())) {
					isSafeSpot = true; 
				}
			}
			obstacles.add(newWall);
		}

		// ---FOG OF WAR LOGIC ---
		Image fogImg = new Image(getClass().getResource("/screens/cloud.png").toExternalForm());
		ImagePattern fogTexture = new ImagePattern(fogImg);
		Rectangle fogBackground = new Rectangle(700, 400, fogTexture);

		RadialGradient visionLight = new RadialGradient(
				0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
				new Stop(0, Color.WHITE),
				new Stop(1, Color.TRANSPARENT) 
				);
		p1Vision = new Circle(110, visionLight);
		p2Vision = new Circle(110, visionLight);
		Group visionMask = new Group(p1Vision, p2Vision);

		// THE GAME WORLD 
		gameWorld = new Group();
		gameWorld.getChildren().add(mapBackground); 
		gameWorld.getChildren().addAll(obstacles);

		for (int i = 0; i < 2; i++) {
			freezeTraps[i] = new Circle(20, Color.CYAN);
			freezeTraps[i].setStroke(Color.WHITE);
			freezeTraps[i].setStrokeWidth(3);
			
			// Applied Settings Check
			if (GameSettings.powerUpsEnabled) {
				spawnTrap(freezeTraps[i]);
			} else {
				freezeTraps[i].setVisible(false);
			}
			gameWorld.getChildren().add(freezeTraps[i]);
		}

		gameWorld.getChildren().addAll(player1, player2);
		gameWorld.getChildren().addAll(player1.getShieldAura(), player2.getShieldAura());
		gameWorld.getChildren().addAll(player1.getNameTag(), player2.getNameTag());

		// Spawn initial power-up if enabled
		if (GameSettings.powerUpsEnabled) {
			spawnPowerUp();
		}

		gameWorld.setBlendMode(BlendMode.SRC_ATOP);

		// sound (Merged AUDIO_ENABLED flag with the new dynamic sfxEnabled Setting)
		if (AUDIO_ENABLED) {
			fuseSound = new AudioClip(getClass().getResource("/music/fuse_music.mp3").toExternalForm());
			explosionSound = new AudioClip(getClass().getResource("/music/explosion_music.mp3").toExternalForm());
			
			if (!GameSettings.sfxEnabled) {
				fuseSound.setVolume(0);
				explosionSound.setVolume(0);
			} else {
				explosionSound.setVolume(1.0);
			}
		}

		Group maskedWorld = new Group(visionMask, gameWorld);
		maskedWorld.setCache(true); 

		this.getChildren().addAll(fogBackground, maskedWorld, timerText, gameOverText, returnButton);
		startGame();
	}

	private void spawnTrap(Circle trap) {
		boolean safe = false;
		while (!safe) {
			double px = Math.random() * (660 - 40) + 20;
			double py = Math.random() * (360 - 40) + 20;
			trap.setCenterX(px);
			trap.setCenterY(py);
			safe = true;
			for (Rectangle wall : obstacles) {
				if (trap.getBoundsInParent().intersects(wall.getBoundsInParent())) {
					safe = false;
					break;
				}
			}
		}
		trap.setVisible(true);
	}

	private void spawnPowerUp() {
		long onBoard = activePowerUps.stream().filter(p -> !p.isCollected()).count();
		if (onBoard >= maxActivePowerUps) return;

		PowerUp.PowerUpType type = Math.random() < 0.5
				? PowerUp.PowerUpType.SPEED : PowerUp.PowerUpType.SHIELD;

		double x = 0, y = 0;
		boolean safe = false;
		int tries = 0;
		while (!safe && tries < 30) {
			x = Math.random() * (700 - 80) + 10;
			y = Math.random() * (400  - 80) + 10;
			Rectangle test = new Rectangle(x, y, 60, 60);
			safe = !test.getBoundsInParent().intersects(player1.getBoundsInParent())
					&& !test.getBoundsInParent().intersects(player2.getBoundsInParent());
			if (safe) {
				for (Rectangle wall : obstacles) {
					if (test.getBoundsInParent().intersects(wall.getBoundsInParent())) {
						safe = false;
						break;
					}
				}
			}
			tries++;
		}
		if (!safe) return; 

		PowerUp pu = new PowerUp(type, x, y);
		activePowerUps.add(pu);
		gameWorld.getChildren().add(pu.getSprite());
	}

	public void addKey(KeyCode code) { activeKeys.add(code); }
	public void removeKey(KeyCode code) { activeKeys.remove(code); }

	private void startGame() {
		gameLoop = new AnimationTimer() {
			long startTime = -1;

			@Override
			public void handle(long now) {
				if (startTime == -1) startTime = now;

				long elapsedNanos = now - startTime;
				long timeRemaining = (gameDurationNanos - elapsedNanos) / 1_000_000_000L;

				if (timeRemaining <= 0) {
					endGame();
					return;
				}

				if (timeRemaining <= 5 && !fusePlayed) {
					if (AUDIO_ENABLED && fuseSound != null) fuseSound.play();
					fusePlayed = true;
				}

				timerText.setText("TIME: " + timeRemaining);
				player1.move(activeKeys, getWidth(), getHeight(), obstacles);
				player2.move(activeKeys, getWidth(), getHeight(), obstacles);

				p1Vision.setCenterX(player1.getCenterX() - 30);
				p1Vision.setCenterY(player1.getCenterY());
				p2Vision.setCenterX(player2.getCenterX() - 30);
				p2Vision.setCenterY(player2.getCenterY());

				if (p1FrozenUntil > 0 && now >= p1FrozenUntil) {
					player1.setFrozen(false);
					p1FrozenUntil = 0;
				}
				if (p2FrozenUntil > 0 && now >= p2FrozenUntil) {
					player2.setFrozen(false);
					p2FrozenUntil = 0;
				}

				if (p1SpeedBoostEnd > 0 && now >= p1SpeedBoostEnd) {
					player1.resetSpeed();
					p1SpeedBoostEnd = 0;
				}
				if (p2SpeedBoostEnd > 0 && now >= p2SpeedBoostEnd) {
					player2.resetSpeed();
					p2SpeedBoostEnd = 0;
				}

				checkCollision(now);
			}
		};
		gameLoop.start();
	}

	private void checkCollision(long now) {
		// --- Bomb pass with shield handling ---
		if (player1.getHitbox().intersects(player2.getHitbox())
				&& now - bombLastPassedTime > cooldownNanos) {

			Player holder   = player1.hasBomb() ? player1 : player2;
			Player receiver = (holder == player1) ? player2 : player1;

			if (receiver.isShielded()) {
				receiver.breakShield();
			} else {
				holder.setBomb(false);
				receiver.setBomb(true);
			}
			bombLastPassedTime = now;
		}
		
		// PowerUp Spawning Logic
		if (GameSettings.powerUpsEnabled && now - lastPowerUpSpawnTime > 5_000_000_000L && Math.random() < 0.05) {
			for (Circle trap : freezeTraps) {
				if (!trap.isVisible()) {
					spawnTrap(trap);
					lastPowerUpSpawnTime = now;
					break;
				}
			}
		}

		// PowerUp Collision Logic (freezes the player who touches it)
		// Merged: Wrapped in GameSettings check, but kept the strict getHitbox() logic from HEAD
		if (GameSettings.powerUpsEnabled) {
			for (Circle trap : freezeTraps) {
				if (trap.isVisible()) {
					if (player1.getHitbox().intersects(trap.getBoundsInParent())) {
						trap.setVisible(false);
						p1FrozenUntil = now + 5_000_000_000L; 
						player1.setFrozen(true);
					} else if (player2.getHitbox().intersects(trap.getBoundsInParent())) {
						trap.setVisible(false);
						p2FrozenUntil = now + 5_000_000_000L; 
						player2.setFrozen(true);
					}
				}
			}
		}

		// --- Periodic power-up spawn ---
		if (GameSettings.powerUpsEnabled && now - lastPowerUpSpawnTime > spawnIntervalNanos) {
			spawnPowerUp();
			lastPowerUpSpawnTime = now;
		}

		// --- Pickup detection ---
		for (PowerUp pu : activePowerUps) {
			if (pu.isCollected()) continue;

			Player collector = null;
			if (player1.getHitbox().intersects(pu.getBounds()))      collector = player1;
			else if (player2.getHitbox().intersects(pu.getBounds())) collector = player2;
			if (collector == null) continue;

			pu.collect();
			gameWorld.getChildren().remove(pu.getSprite());

			switch (pu.getType()) {
			case SPEED  -> applySpeedBoost(collector, now);
			case SHIELD -> collector.setShielded(true);
			}
		}
		activePowerUps.removeIf(PowerUp::isCollected);
	}

	private void applySpeedBoost(Player p, long now) {
		p.applySpeedBoost(1.8);
		if (p == player1) p1SpeedBoostEnd = now + speedBoostDurationNanos;
		else              p2SpeedBoostEnd = now + speedBoostDurationNanos;
	}

	private void endGame() {
		gameLoop.stop();
		if (AUDIO_ENABLED) {
			if (explosionSound != null) explosionSound.play();
			if (fuseSound != null) fuseSound.stop();
		}

		player1.resetSpeed();
		player2.resetSpeed();
		player1.setShielded(false);
		player2.setShielded(false);

		Platform.runLater(() -> {

			timerText.setText("TIME: 0");

			Player loser = player1.hasBomb() ? player1 : player2;

			String gifPath = "/sprites/die/with_bomb_front.gif";

			switch (loser.getFacing()) {
			case LEFT:
				gifPath = "/sprites/die/with_bomb_left.gif";
				break;
			case RIGHT:
				gifPath = "/sprites/die/with_bomb_right.gif";
				break;
			case UP:
				gifPath = "/sprites/die/with_bomb_back.gif";
				break;
			case DOWN:
				gifPath = "/sprites/die/with_bomb_front.gif";
				break;
			}

			Image gif = new Image(getClass().getResource(gifPath).toExternalForm());
			deathGif = new ImageView(gif);

			deathGif.setFitWidth(60);
			deathGif.setFitHeight(60);

			deathGif.setX(loser.getX());
			deathGif.setY(loser.getY());

			loser.setVisible(false);
			loser.getNameTag().setVisible(false);

			getChildren().add(deathGif);

			gameOverText.toFront();
			returnButton.toFront();

			gameOverText.setVisible(true);
			returnButton.setVisible(true);

			if (player1.hasBomb()) {
				gameOverText.setText("TIME'S UP! PLAYER 2 WINS!");
				gameOverText.setFill(Color.rgb(50,255,50));
			} else {
				gameOverText.setText("TIME'S UP! PLAYER 1 WINS!");
				gameOverText.setFill(Color.rgb(50,150,255));
			}

			gameOverText.setX((700 - gameOverText.getLayoutBounds().getWidth()) / 2);

		}); 
	}
}