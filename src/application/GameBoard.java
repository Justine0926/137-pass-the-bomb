package application;

import java.util.HashSet;
import java.util.Set;

import javafx.animation.AnimationTimer;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import javafx.scene.effect.BlendMode;
import javafx.scene.Group;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Circle;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;

public class GameBoard extends Pane {
    private final Player player1;
    private final Player player2;
    private final Text timerText;
    private final Text gameOverText;
    private final Button returnButton; 
    
    private final Set<KeyCode> activeKeys = new HashSet<>();
    private AnimationTimer gameLoop;
    
    private final long gameDurationNanos = 5_000_000_000L;
    private final long cooldownNanos = 1_000_000_000L;
    private long bombLastPassedTime = 0;
    
    // limited vision variables
    private final Group darknessLayer;
    private final Circle p1Vision;
    private final Circle p2Vision;
    
    //randomized bomb holder
    boolean bombHolder = Math.random() < 0.5;
    
    // We now pass a Runnable so the board knows how to go back to the menu
    public GameBoard(Runnable onMenuReturn) {
        player1 = new Player(200, 300, Color.DODGERBLUE, 
                             KeyCode.W, KeyCode.S, KeyCode.A, KeyCode.D, bombHolder);

        player2 = new Player(600, 300, Color.LIMEGREEN, 
                             KeyCode.UP, KeyCode.DOWN, KeyCode.LEFT, KeyCode.RIGHT, !bombHolder);

        timerText = new Text(350, 50, "Time: 60");
        timerText.setFont(Font.font("Arial", FontWeight.BOLD, 30));

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
        // Create a gradient for the "Flashlight" (White in the center, fades to Black at the edges)
        RadialGradient visionLight = new RadialGradient(
            0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.WHITE),
            new Stop(1, Color.BLACK)
        );

        // Create the vision circles (150 is the radius/size of their vision)
        p1Vision = new Circle(150, visionLight);
        p2Vision = new Circle(150, visionLight);

        // Create a pitch-black background
        Rectangle darkBackground = new Rectangle(800, 600, Color.BLACK);

        // Group them together and apply the Multiply blend mode
        darknessLayer = new Group(darkBackground, p1Vision, p2Vision);
        darknessLayer.setBlendMode(BlendMode.MULTIPLY);
        
        // add everything on the screen
        this.getChildren().addAll(player1, player2, darknessLayer, timerText, gameOverText, returnButton);
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
                player1.move(activeKeys, getWidth(), getHeight());
                player2.move(activeKeys, getWidth(), getHeight());

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