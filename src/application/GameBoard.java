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
	private AudioClip fuseSound;
	private AudioClip explosionSound;
	private boolean fusePlayed = false;

	// obstacles
	private final List<Rectangle> obstacles = new ArrayList<>();

	//randomized bomb holder
	boolean bombHolder = Math.random() < 0.5;

	// powerup logic
	private final Circle[] freezeTraps = new Circle[2];
	private long p1FrozenUntil = 0;
	private long p2FrozenUntil = 0;
	private long lastPowerUpSpawnTime = 0;

	// we now pass a Runnable so the board knows how to go back to the menu
	public GameBoard(Runnable onMenuReturn) {
		player1 = new Player(200, 300, 
				KeyCode.W, KeyCode.S, KeyCode.A, KeyCode.D, bombHolder, "PLAYER 1");

		player2 = new Player(1100, 300, 
				KeyCode.UP, KeyCode.DOWN, KeyCode.LEFT, KeyCode.RIGHT, !bombHolder, "PLAYER 2");

		// --- TIMER TEXT ---
		timerText = new Text(650, 50, "TIME: 90");
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
		gameOverText = new Text(600, 400, "");
		gameOverText.setFont(Font.font("Monospaced", FontWeight.BOLD, 50));
		gameOverText.setStroke(Color.rgb(0x08, 0x08, 0x08)); 
		gameOverText.setStrokeWidth(3); // Slightly thicker outline for bigger text

		DropShadow gameOverShadow = new DropShadow();
		gameOverShadow.setOffsetY(6);
		gameOverShadow.setOffsetX(6);
		gameOverShadow.setColor(Color.color(0, 0, 0, 0.8));
		gameOverShadow.setRadius(0);
		gameOverText.setEffect(gameOverShadow);

		gameOverText.setVisible(false);

		// ---RETURN BUTTON ---
		returnButton = new Button("BACK TO MAIN MENU"); 
		returnButton.setFont(Font.font("Monospaced", FontWeight.BOLD, 22)); // Matches MENU_FONT_SIZE
		returnButton.setStyle("-fx-background-color: transparent; -fx-text-fill: rgb(144, 140, 128); -fx-cursor: hand;");
		returnButton.setPrefWidth(400);
		returnButton.setLayoutX(500); 
		returnButton.setLayoutY(420); 
		returnButton.setVisible(false); 
		DropShadow buttonShadow = new DropShadow();
		buttonShadow.setOffsetY(3);
		buttonShadow.setOffsetX(3);
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
		mapBackground.setFitWidth(1400);
		mapBackground.setFitHeight(800);

		// --- RANDOM OBSTACLE SETUP ---
		int numberOfObstacles = 6; 
		double obstacleSize = 80; //obstacle size

		//load image
		Image crateImg = new Image(getClass().getResource("/sprites/obstacle/fence.png").toExternalForm());
		ImagePattern crateTexture = new ImagePattern(crateImg);

		for (int i = 0; i < numberOfObstacles; i++) {
			boolean isSafeSpot = false;
			Rectangle newWall = null;

			while (!isSafeSpot) {
				// randomize position 
				double randomX = Math.random() * (1360 - obstacleSize);
				double randomY = Math.random() * (800 - obstacleSize);

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
		Rectangle fogBackground = new Rectangle(1400, 800, fogTexture);

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

		// Setup powerup circles
		for (int i = 0; i < 2; i++) {
			freezeTraps[i] = new Circle(20, Color.CYAN);
			freezeTraps[i].setStroke(Color.WHITE);
			freezeTraps[i].setStrokeWidth(3);
			spawnTrap(freezeTraps[i]);
			gameWorld.getChildren().add(freezeTraps[i]);
		}

		gameWorld.getChildren().addAll(player1, player2);
		gameWorld.getChildren().addAll(player1.getNameTag(), player2.getNameTag());
		// inside the white pixels of the visionMask!
		gameWorld.setBlendMode(BlendMode.SRC_ATOP);

		//sound
		fuseSound = new AudioClip(getClass().getResource("/music/fuse_music.wav").toExternalForm());
		explosionSound = new AudioClip(getClass().getResource("/music/explosion_music.wav").toExternalForm());

		explosionSound.setVolume(1.0);

		// package the mask and the game world together and turn on caching. 
		// This forces JavaFX to cut the holes properly before placing it over the clouds.
		Group maskedWorld = new Group(visionMask, gameWorld);
		maskedWorld.setCache(true); 

		// ADD TO SCREEN
		this.getChildren().addAll(fogBackground, maskedWorld, timerText, gameOverText, returnButton);

		startGame();
	}

	private void spawnTrap(Circle trap) {
		boolean safe = false;
		while (!safe) {
			double px = Math.random() * (1360 - 40) + 20;
			double py = Math.random() * (760 - 40) + 20;
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
					fuseSound.play();
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

				// unfreeze logic
				if (p1FrozenUntil > 0 && now >= p1FrozenUntil) {
					player1.setFrozen(false);
					p1FrozenUntil = 0;
				}
				if (p2FrozenUntil > 0 && now >= p2FrozenUntil) {
					player2.setFrozen(false);
					p2FrozenUntil = 0;
				}

				checkCollision(now);
			}
		};
		gameLoop.start();
	}

	//  collision checker
	private void checkCollision(long now) {
		if (player1.getBoundsInParent().intersects(player2.getBoundsInParent())) {
			// pass the bomb will only work after the invincible time
			if (now - bombLastPassedTime > cooldownNanos) {
				boolean p1HadBomb = player1.hasBomb();
				player1.setBomb(!p1HadBomb);
				player2.setBomb(p1HadBomb);
				bombLastPassedTime = now;
			}
		}

		// PowerUp Spawning Logic
		if (now - lastPowerUpSpawnTime > 5_000_000_000L && Math.random() < 0.05) {
			for (Circle trap : freezeTraps) {
				if (!trap.isVisible()) {
					spawnTrap(trap);
					lastPowerUpSpawnTime = now;
					break;
				}
			}
		}

		// PowerUp Collision Logic (freezes the player who touches it)
		for (Circle trap : freezeTraps) {
			if (trap.isVisible()) {
				if (player1.getBoundsInParent().intersects(trap.getBoundsInParent())) {
					trap.setVisible(false);
					p1FrozenUntil = now + 5_000_000_000L; // 5 seconds
					player1.setFrozen(true);
				} else if (player2.getBoundsInParent().intersects(trap.getBoundsInParent())) {
					trap.setVisible(false);
					p2FrozenUntil = now + 5_000_000_000L; // 5 seconds
					player2.setFrozen(true);
				}
			}
		}
	}

	//    end game screen
	private void endGame() {
		// 1. Stop the game loop and handle audio immediately
		gameLoop.stop();
		explosionSound.play();
		fuseSound.stop();

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

			deathGif.setFitWidth(120);
			deathGif.setFitHeight(120);

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
			gameOverText.setX((1400 - gameOverText.getLayoutBounds().getWidth()) / 2);

		}); // End of Platform.runLater
	}
}