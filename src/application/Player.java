package application;

import java.util.List;
import java.util.Set;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.shape.Rectangle;

public class Player extends ImageView {
    private boolean hasBomb;
    private boolean isMoving = false;
    private final int speed = 6;
    private final double size = 40; 
    
    // Arrays to hold the 3 frames of walking animation
    private final Image idleImg;
    private final Image idleBombImg;
    private final Image[] walkFrames = new Image[3];
    private final Image[] walkBombFrames = new Image[3];
    
    // Animation counters
    private int currentWalkFrame = 0;
    private int animationTick = 0;
    private final int animationSpeed = 8; // Higher number = slower leg movement
    
    private final KeyCode upKey, downKey, leftKey, rightKey;

    public Player(double startX, double startY, String prefix, 
                  KeyCode up, KeyCode down, KeyCode left, KeyCode right, boolean startsWithBomb) {
        
        // 1. Load the stationary images
        idleImg = new Image(getClass().getResource("/sprites/" + prefix + "idle.png").toExternalForm());
        idleBombImg = new Image(getClass().getResource("/sprites/" + prefix + "idle_bomb.png").toExternalForm());
        
        // 2. Loop to load the 3 walking frames dynamically
        for (int i = 0; i < 3; i++) {
            // (i + 1) makes it look for walk1.png, walk2.png, walk3.png
            walkFrames[i] = new Image(getClass().getResource("/sprites/" + prefix + "walk" + (i + 1) + ".png").toExternalForm());
            walkBombFrames[i] = new Image(getClass().getResource("/sprites/" + prefix + "walk_bomb" + (i + 1) + ".png").toExternalForm());
        }
        
        this.setX(startX);
        this.setY(startY);
        this.setFitWidth(size);
        this.setFitHeight(size);
        
        this.upKey = up;
        this.downKey = down;
        this.leftKey = left;
        this.rightKey = right;
        this.hasBomb = startsWithBomb;
        
        updateAppearance();
    }

    public void move(Set<KeyCode> activeKeys, double screenWidth, double screenHeight, List<Rectangle> obstacles) {
        double oldX = getX();
        double oldY = getY();
        
        boolean wasMoving = isMoving;
        isMoving = false; 

        // Attempt movement
        if (activeKeys.contains(upKey) && getY() > 0) {
            setY(getY() - speed);
            isMoving = true;
        }
        if (activeKeys.contains(downKey) && getY() + size < screenHeight) {
            setY(getY() + speed);
            isMoving = true;
        }
        if (activeKeys.contains(leftKey) && getX() > 0) {
            setX(getX() - speed);
            isMoving = true;
        }
        if (activeKeys.contains(rightKey) && getX() + size < screenWidth) {
            setX(getX() + speed);
            isMoving = true;
        }

        // Check wall collision
        for (Rectangle wall : obstacles) {
            if (this.getBoundsInParent().intersects(wall.getBoundsInParent())) {
                setX(oldX);
                setY(oldY);
                break; 
            }
        }
        
        // --- NEW ANIMATION LOGIC ---
        if (isMoving) {
            animationTick++;
            // If enough time has passed, step to the next frame
            if (animationTick >= animationSpeed) {
                animationTick = 0; // Reset the metronome
                
                // Move to next frame, loop back to 0 if it hits 3
                currentWalkFrame = (currentWalkFrame + 1) % 3; 
                updateAppearance();
            }
        } else {
            // If they stop walking, instantly reset their legs to the starting position
            currentWalkFrame = 0;
            animationTick = 0;
        }

        // Catch the exact moment they start or stop walking to ensure it updates immediately
        if (wasMoving != isMoving) {
            updateAppearance();
        }
    }

    public boolean hasBomb() {
        return hasBomb;
    }

    public void setBomb(boolean hasBomb) {
        this.hasBomb = hasBomb;
        updateAppearance();
    }

    private void updateAppearance() {
        if (hasBomb) {
            // Use the array and the current frame index!
            this.setImage(isMoving ? walkBombFrames[currentWalkFrame] : idleBombImg);
        } else {
            this.setImage(isMoving ? walkFrames[currentWalkFrame] : idleImg);
        }
    }
    
    // Helper Methods
    public double getCenterX() { return getX() + (size / 2); }
    public double getCenterY() { return getY() + (size / 2); }
}