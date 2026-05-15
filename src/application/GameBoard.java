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

//THIS IS THE CLASS FOR THE GAMEBOARD/MAP
//where the game takes place
public class GameBoard extends Pane {
	//initialize players
	private final Player player1;
	private final Player player2;
	private Group gameWorld;

	//screen layout
	private final Text timerText;
	private final Text gameOverText;
	private final Button returnButton; 

	//moves storage
	private final Set<KeyCode> activeKeys = new HashSet<>();
	private AnimationTimer gameLoop;
	private ImageView deathGif;

	private final long gameDurationNanos = 90_000_000_000L;
	private final long cooldownNanos = 1_000_000_000L;
	private long bombLastPassedTime = 0;

	// limited vision variables
	//    private final Group darknessLayer;
	private final Circle p1Vision;
	private final Circle p2Vision;

	//sound
	// TEMP: disabled until JavaFX media playback is restored on Fedora 43.
	// Flip AUDIO_ENABLED to re-enable.
	private static final boolean AUDIO_ENABLED = false;
	private AudioClip fuseSound;
	private AudioClip explosionSound;
	private boolean fusePlayed = false;

	// obstacles
	private final List<Rectangle> obstacles = new ArrayList<>();

	//randomized bomb holder
	boolean bombHolder = Math.random() < 0.5;

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
		timerText = new Text(300, 50, "TIME: 90");
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
		gameOverText.setStrokeWidth(1); // Slightly thicker outline for bigger text

		DropShadow gameOverShadow = new DropShadow();
		gameOverShadow.setOffsetY(6);
		gameOverShadow.setOffsetX(6);
		gameOverShadow.setColor(Color.color(0, 0, 0, 0.8));
		gameOverShadow.setRadius(0);
		gameOverText.setEffect(gameOverShadow);

		gameOverText.setVisible(false);

		// ---RETURN BUTTON ---
		returnButton = new Button("BACK TO MAIN MENU"); 
		returnButton.setFont(Font.font("Monospaced", FontWeight.BOLD, 18)); // Matches MENU_FONT_SIZE
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
		// When the mouse enters, change color to ACCENT (rgb(232, 224, 192))
		returnButton.setOnMouseEntered(e -> {
			returnButton.setStyle("-fx-background-color: transparent; -fx-text-fill: rgb(232, 224, 192); -fx-cursor: hand;");
		});

		// When the mouse leaves, change back to DIM_WHITE (rgb(144, 140, 128))
		returnButton.setOnMouseExited(e -> {
			returnButton.setStyle("-fx-background-color: transparent; -fx-text-fill: rgb(144, 140, 128); -fx-cursor: hand;");
		});

		// when clicked, run the menu method
		returnButton.setOnAction(e -> onMenuReturn.run());

		// --- BACKGROUND ARENA LOGIC ---
		// get the image file in the screens folder
		Image mapImage = new Image(getClass().getResource("/screens/arena.png").toExternalForm());

		// place the image
		ImageView mapBackground = new ImageView(mapImage);

		// stretch the image to fill the exact size of our window
		mapBackground.setFitWidth(700);
		mapBackground.setFitHeight(400);

		// --- RANDOM OBSTACLE SETUP ---
		int numberOfObstacles = 6; 
		double obstacleSize = 40; //obstacle size

		//load image
		Image crateImg = new Image(getClass().getResource("/sprites/obstacle/fence.png").toExternalForm());
		ImagePattern crateTexture = new ImagePattern(crateImg);

		for (int i = 0; i < numberOfObstacles; i++) {
			boolean isSafeSpot = false;
			Rectangle newWall = null;

			while (!isSafeSpot) {
				// randomize position 
				double randomX = Math.random() * (660 - obstacleSize);
				double randomY = Math.random() * (400 - obstacleSize);

				// build the physical hitbox
				newWall = new Rectangle(randomX, randomY, obstacleSize, obstacleSize);

				// paint the rectangle with your image instead of Color.DARKGRAY
				newWall.setFill(crateTexture); 

				// ensure the wall doesn't spawn on top of Player 1 or Player 2
				if (!newWall.getBoundsInParent().intersects(player1.getBoundsInParent()) && 
						!newWall.getBoundsInParent().intersects(player2.getBoundsInParent())) {

					isSafeSpot = true; 
				}
			}

			obstacles.add(newWall);
		}

		// ---FOG OF WAR LOGIC ---

		// THE CLOUDS (Bottom Layer)
		// draw the clouds normally at the very back of the screen.
		Image fogImg = new Image(getClass().getResource("/screens/cloud.png").toExternalForm());
		ImagePattern fogTexture = new ImagePattern(fogImg);
		Rectangle fogBackground = new Rectangle(700, 400, fogTexture);

		// The Flashlights
		// White acts as "Visible", Transparent acts as "Invisible"
		RadialGradient visionLight = new RadialGradient(
				0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
				new Stop(0, Color.WHITE),
				new Stop(1, Color.TRANSPARENT) 
				);
		p1Vision = new Circle(110, visionLight);
		p2Vision = new Circle(110, visionLight);
		Group visionMask = new Group(p1Vision, p2Vision);

		// THE GAME WORLD 
		// group everything that should be hidden by the fog into one container
		gameWorld = new Group();
		
		gameWorld.getChildren().add(mapBackground); 
		gameWorld.getChildren().addAll(obstacles);

		gameWorld.getChildren().addAll(player1, player2);
		gameWorld.getChildren().addAll(player1.getShieldAura(), player2.getShieldAura());
		gameWorld.getChildren().addAll(player1.getNameTag(), player2.getNameTag());

		// spawn one initial power-up so the board isn't empty
		spawnPowerUp();
		// inside the white pixels of the visionMask!
		gameWorld.setBlendMode(BlendMode.SRC_ATOP);

		//sound
		if (AUDIO_ENABLED) {
			fuseSound = new AudioClip(getClass().getResource("/music/fuse_music.mp3").toExternalForm());
			explosionSound = new AudioClip(getClass().getResource("/music/explosion_music.mp3").toExternalForm());
			explosionSound.setVolume(1.0);
		}

		// package the mask and the game world together and turn on caching. 
		// This forces JavaFX to cut the holes properly before placing it over the clouds.
		Group maskedWorld = new Group(visionMask, gameWorld);
		maskedWorld.setCache(true); 

		// ADD TO SCREEN
		this.getChildren().addAll(fogBackground, maskedWorld, timerText, gameOverText, returnButton);

		startGame();
	}

	private void spawnPowerUp() {
		// cap concurrent power-ups on the board
		long onBoard = activePowerUps.stream().filter(p -> !p.isCollected()).count();
		if (onBoard >= maxActivePowerUps) return;

		// 50/50 between SPEED and SHIELD
		PowerUp.PowerUpType type = Math.random() < 0.5
				? PowerUp.PowerUpType.SPEED
				: PowerUp.PowerUpType.SHIELD;

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
		if (!safe) return; // give up this tick; we'll try again next interval

		PowerUp pu = new PowerUp(type, x, y);
		activePowerUps.add(pu);
		gameWorld.getChildren().add(pu.getSprite());
	}

	public void addKey(KeyCode code) { activeKeys.add(code); }
	public void removeKey(KeyCode code) { activeKeys.remove(code); }

	// method for starting the game
	private void startGame() {
		gameLoop = new AnimationTimer() {
			long startTime = -1;

			@Override
			public void handle(long now) {
				if (startTime == -1) startTime = now;

				long elapsedNanos = now - startTime;
				long timeRemaining = (gameDurationNanos - elapsedNanos) / 1_000_000_000L;

				//                end game if there is no time remaining
				if (timeRemaining <= 0) {
					endGame();
					return;
				}

				if (timeRemaining <= 5 && !fusePlayed) {
					if (AUDIO_ENABLED && fuseSound != null) fuseSound.play();
					fusePlayed = true;
				}

				timerText.setText("TIME: " + timeRemaining);
				// move players
				player1.move(activeKeys, getWidth(), getHeight(), obstacles);
				player2.move(activeKeys, getWidth(), getHeight(), obstacles);

				// make the vision circles follow the players
				p1Vision.setCenterX(player1.getCenterX() - 30);
				p1Vision.setCenterY(player1.getCenterY());
				p2Vision.setCenterX(player2.getCenterX() - 30);
				p2Vision.setCenterY(player2.getCenterY());

				// speed-boost expiry per player
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

	//  collision checker
	private void checkCollision(long now) {
		// --- Bomb pass with shield handling ---
		if (player1.getBoundsInParent().intersects(player2.getBoundsInParent())
				&& now - bombLastPassedTime > cooldownNanos) {

			Player holder   = player1.hasBomb() ? player1 : player2;
			Player receiver = (holder == player1) ? player2 : player1;

			if (receiver.isShielded()) {
				// shield absorbs this would-be pass and breaks
				receiver.breakShield();
			} else {
				holder.setBomb(false);
				receiver.setBomb(true);
			}
			bombLastPassedTime = now;
		}

		// --- Periodic power-up spawn ---
		if (now - lastPowerUpSpawnTime > spawnIntervalNanos) {
			spawnPowerUp();
			lastPowerUpSpawnTime = now;
		}

		// --- Pickup detection ---
		for (PowerUp pu : activePowerUps) {
			if (pu.isCollected()) continue;

			Player collector = null;
			if (player1.getBoundsInParent().intersects(pu.getBounds()))      collector = player1;
			else if (player2.getBoundsInParent().intersects(pu.getBounds())) collector = player2;
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

	//    end game screen
	private void endGame() {
		// 1. Stop the game loop and handle audio immediately
		gameLoop.stop();
		if (AUDIO_ENABLED) {
			if (explosionSound != null) explosionSound.play();
			if (fuseSound != null) fuseSound.stop();
		}

		// clear lingering power-up effects so visuals don't persist on the death screen
		player1.resetSpeed();
		player2.resetSpeed();
		player1.setShielded(false);
		player2.setShielded(false);

		// 2. Queue all visual UI updates and scene graph mutations safely
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

			//hide the loser instead of deleting them so we don't break the cache!
			loser.setVisible(false);
			loser.getNameTag().setVisible(false);

			// add the death GIF directly to the main screen, OVER the fog!
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

			// Recalculate layout dynamically
			gameOverText.setX((700 - gameOverText.getLayoutBounds().getWidth()) / 2);

		}); // End of Platform.runLater
	}
}