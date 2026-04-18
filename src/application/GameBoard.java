package application;

import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

import javafx.animation.AnimationTimer;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.effect.BlendMode;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

// THIS IS THE CLASS FOR THE GAMEBOARD/MAP
// where the game takes place

public class GameBoard extends Pane {
	//initialize players
    private final Player player1;
    private final Player player2;
    
    //screen layout
    private final Text timerText;
    private final Text gameOverText;
    private final Button returnButton; 
    
    //moves storage
    private final Set<KeyCode> activeKeys = new HashSet<>();
    private AnimationTimer gameLoop;
    
    private final long gameDurationNanos = 5_000_000_000L;
    private final long cooldownNanos = 1_000_000_000L;
    private long bombLastPassedTime = 0;
    
    // limited vision variables
    private final Group darknessLayer;
    private final Circle p1Vision;
    private final Circle p2Vision;
    
    // obstacles
    private final List<Rectangle> obstacles = new ArrayList<>();
    
    //randomized bomb holder
    boolean bombHolder = Math.random() < 0.5;
    
    // we now pass a Runnable so the board knows how to go back to the menu
    public GameBoard(Runnable onMenuReturn) {
        player1 = new Player(200, 300, Color.DODGERBLUE, 
                             KeyCode.W, KeyCode.S, KeyCode.A, KeyCode.D, bombHolder);

        player2 = new Player(600, 300, Color.LIMEGREEN, 
                             KeyCode.UP, KeyCode.DOWN, KeyCode.LEFT, KeyCode.RIGHT, !bombHolder);

        timerText = new Text(350, 50, "Time: 60");
        timerText.setFont(Font.font("Arial", FontWeight.BOLD, 30));
        timerText.setFill(Color.ORANGE);

        gameOverText = new Text(200, 300, "");
        gameOverText.setFont(Font.font("Arial", FontWeight.BOLD, 40));
        gameOverText.setVisible(false);

        // setup the Return to Menu Button
        returnButton = new Button("Back to Main Menu");
        returnButton.setFont(Font.font("Arial", 20));
        returnButton.setPrefWidth(220);
        returnButton.setLayoutX(290); // Centered horizontally
        returnButton.setLayoutY(380); // Placed right below the Game Over text
        returnButton.setVisible(false); // Hidden while playing
        
        
        // when clicked, run the menu method
        returnButton.setOnAction(e -> onMenuReturn.run());
        
        // --- LIMITED VISION LOGIC ---
        // create a gradient for the "Flashlight" (White in the center, fades to Black at the edges)
        RadialGradient visionLight = new RadialGradient(
            0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.WHITE),
            new Stop(1, Color.BLACK)
        );

        // create the vision circles
        p1Vision = new Circle(75, visionLight);
        p2Vision = new Circle(75, visionLight);

        // create a pitch-black background
        Rectangle darkBackground = new Rectangle(800, 600, Color.BLACK);

        // group them together and apply the Multiply blend mode
        darknessLayer = new Group(darkBackground, p1Vision, p2Vision);
        darknessLayer.setBlendMode(BlendMode.MULTIPLY);
        
     // --- RANDOM OBSTACLE SETUP ---
        int numberOfObstacles = 5; // arbitrary number of obstacles
        
        for (int i = 0; i < numberOfObstacles; i++) {
            boolean isSafeSpot = false;
            Rectangle newWall = null;
            
            // keep guessing random locations until we find one that doesn't trap a player
            while (!isSafeSpot) {
                // randomize size (between 40 and 120 pixels)
                double wallWidth = 40 + (Math.random() * 80);
                double wallHeight = 40 + (Math.random() * 80);
                
                // randomize position (Ensuring they stay within the 800x600 screen bounds)
                double randomX = Math.random() * (800 - wallWidth);
                double randomY = Math.random() * (600 - wallHeight);
                
                newWall = new Rectangle(randomX, randomY, wallWidth, wallHeight);
                newWall.setFill(Color.DARKGRAY);
                
                //ensure the wall doesn't spawn on top of Player 1 or Player 2
                if (!newWall.getBoundsInParent().intersects(player1.getBoundsInParent()) && 
                    !newWall.getBoundsInParent().intersects(player2.getBoundsInParent())) {
                    
                    isSafeSpot = true; // safe, break the loop
                }
            }
            
            // add the safely placed wall to the list
            obstacles.add(newWall);
        }
        
        // add everything on the screen
        this.getChildren().addAll(player1, player2);
        this.getChildren().addAll(obstacles); // add walls before the darkness
        this.getChildren().addAll(darknessLayer, timerText, gameOverText, returnButton);
        startGame();
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
                
                timerText.setText("Time: " + timeRemaining);
                // move players
                player1.move(activeKeys, getWidth(), getHeight(), obstacles);
                player2.move(activeKeys, getWidth(), getHeight(), obstacles);

                // make the vision circles follow the players
                p1Vision.setCenterX(player1.getCenterX());
                p1Vision.setCenterY(player1.getCenterY());
                p2Vision.setCenterX(player2.getCenterX());
                p2Vision.setCenterY(player2.getCenterY());
                
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
    }
    
//    end game screen
    private void endGame() {
        gameLoop.stop();
        timerText.setText("Time: 0");
        gameOverText.setVisible(true);
        returnButton.setVisible(true); 
        
        if (player1.hasBomb()) {
            gameOverText.setText("Time's Up! Player 2 Wins!");
            gameOverText.setFill(Color.LIMEGREEN);
        } else {
            gameOverText.setText("Time's Up! Player 1 Wins!");
            gameOverText.setFill(Color.DODGERBLUE);
        }
        
        gameOverText.setX((getWidth() - gameOverText.getLayoutBounds().getWidth()) / 2);
    }
}