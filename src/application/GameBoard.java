package application;

import javafx.animation.AnimationTimer;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.HashSet;
import java.util.Set;

public class GameBoard extends Pane {
    private final Player player1;
    private final Player player2;
    private final Text timerText;
    private final Text gameOverText;
    
    private final Set<KeyCode> activeKeys = new HashSet<>();
    private AnimationTimer gameLoop;
    
    private final long gameDurationNanos = 60_000_000_000L; // 60 seconds
    private final long cooldownNanos = 1_000_000_000L;      // 1 second pass cooldown
    private long bombLastPassedTime = 0;

    public GameBoard() {
        // Setup Player 1 (Blue, WASD keys, Starts WITH the bomb)
        player1 = new Player(200, 300, Color.DODGERBLUE, 
                             KeyCode.W, KeyCode.S, KeyCode.A, KeyCode.D, true);

        // Setup Player 2 (Green, Arrow keys, Starts WITHOUT the bomb)
        player2 = new Player(600, 300, Color.LIMEGREEN, 
                             KeyCode.UP, KeyCode.DOWN, KeyCode.LEFT, KeyCode.RIGHT, false);

        // Setup Timer UI
        timerText = new Text(350, 50, "Time: 60");
        timerText.setFont(Font.font("Arial", FontWeight.BOLD, 30));

        // Setup Game Over UI (Hidden initially)
        gameOverText = new Text(200, 300, "");
        gameOverText.setFont(Font.font("Arial", FontWeight.BOLD, 40));
        gameOverText.setVisible(false);

        this.getChildren().addAll(player1, player2, timerText, gameOverText);
        startGame();
    }

    public void addKey(KeyCode code) { activeKeys.add(code); }
    public void removeKey(KeyCode code) { activeKeys.remove(code); }

    private void startGame() {
        gameLoop = new AnimationTimer() {
            long startTime = -1;

            @Override
            public void handle(long now) {
                if (startTime == -1) startTime = now;

                // 1. Calculate time remaining
                long elapsedNanos = now - startTime;
                long timeRemaining = (gameDurationNanos - elapsedNanos) / 1_000_000_000L;
                
                if (timeRemaining <= 0) {
                    endGame();
                    return;
                }
                
                timerText.setText("Time: " + timeRemaining);

                // 2. Move players
                player1.move(activeKeys, getWidth(), getHeight());
                player2.move(activeKeys, getWidth(), getHeight());

                // 3. Check for collisions (Bomb passing logic)
                checkCollision(now);
            }
        };
        gameLoop.start();
    }

    private void checkCollision(long now) {
        // If players are touching
        if (player1.getBoundsInParent().intersects(player2.getBoundsInParent())) {
            
            // Only pass if 1 second has elapsed since the last pass
            if (now - bombLastPassedTime > cooldownNanos) {
                
                // Swap bomb status
                boolean p1HadBomb = player1.hasBomb();
                player1.setBomb(!p1HadBomb);
                player2.setBomb(p1HadBomb);
                
                // Reset cooldown
                bombLastPassedTime = now;
            }
        }
    }

    private void endGame() {
        gameLoop.stop();
        timerText.setText("Time: 0");
        gameOverText.setVisible(true);
        
        if (player1.hasBomb()) {
            gameOverText.setText("Time's Up! Player 2 Wins!");
            gameOverText.setFill(Color.LIMEGREEN);
        } else {
            gameOverText.setText("Time's Up! Player 1 Wins!");
            gameOverText.setFill(Color.DODGERBLUE);
        }
        
        // Center the game over text roughly
        gameOverText.setX((getWidth() - gameOverText.getLayoutBounds().getWidth()) / 2);
    }
}