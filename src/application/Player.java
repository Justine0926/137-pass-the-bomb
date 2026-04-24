package application;

import java.util.List;
import java.util.Set;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.shape.Rectangle;

public class Player extends ImageView {
    
	//enum for directions
    public enum Direction { DOWN, LEFT, RIGHT, UP }
    
    private boolean hasBomb;
    private boolean isMoving = false;
    private Direction facing = Direction.DOWN; 
    
    private final int xspeed = 5;
    private final int yspeed = 4;
    private final double size = 120; 
    
    // arrays for images (0=Down, 1=Left, 2=Right, 3=Up)
    private final Image[] idleFrames = new Image[4];
    private final Image[] bombIdleFrames = new Image[4];
    private final Image[][] walkFrames = new Image[4][3];
    private final Image[][] bombWalkFrames = new Image[4][3];
    
    private int currentWalkFrame = 0;
    private int animationTick = 0;
    private final int animationSpeedX = 12; // animation for Left/Right
    private final int animationSpeedY = 10; // animation for Up/Down
    
    private final KeyCode upKey, downKey, leftKey, rightKey;

    public Player(double startX, double startY, String prefix, 
                  KeyCode up, KeyCode down, KeyCode left, KeyCode right, boolean startsWithBomb) {
        
        String[] dirs = {"down", "left", "right", "up"};
        
        for (int d = 0; d < 4; d++) {
            String dir = dirs[d];
            
            // load Idle Images
            // matches: /idle/idle_down.png
            idleFrames[d] = safeLoad("/sprites/idle/idle_" + dir + ".png");
            
            // matches: /idle_bomb/bomb_idle_down.png
            bombIdleFrames[d] = safeLoad("/sprites/idle_bomb/bomb_idle_" + dir + ".png");
            
            // load Walking Images
            for (int f = 0; f < 3; f++) {
                int frameNum = f + 1;
                
                // matches: /walking/down/walk_down1.png
                walkFrames[d][f] = safeLoad("/sprites/walking/" + dir + "/walk_" + dir + frameNum + ".png");
                
                // matches: /walking_bomb/left/bomb_walk_left1.png
                bombWalkFrames[d][f] = safeLoad("/sprites/walking_bomb/" + dir + "/bomb_walk_" + dir + frameNum + ".png");
            }
        }
        
        this.setX(startX);
        this.setY(startY);
        this.setFitWidth(size);
        this.setFitHeight(size);
        this.setPreserveRatio(true);
        this.setSmooth(false);
        
        this.upKey = up;
        this.downKey = down;
        this.leftKey = left;
        this.rightKey = right;
        this.hasBomb = startsWithBomb;
        
        updateAppearance();
    }

    // --- SAFETY NET METHOD ---
    // if a file is missing (like your empty down/up bomb folders), it returns null 
    // instead of crashing the entire game.
    private Image safeLoad(String path) {
        java.net.URL url = getClass().getResource(path);
        if (url == null) {
            System.err.println("WARNING: Could not find image -> " + path);
            return null; 
        }
        return new Image(url.toExternalForm());
    }

    public void move(Set<KeyCode> activeKeys, double screenWidth, double screenHeight, List<Rectangle> obstacles) {
        double oldX = getX();
        double oldY = getY();
        
        boolean wasMoving = isMoving;
        Direction oldFacing = facing;
        isMoving = false; 

        if (activeKeys.contains(upKey) && getY() > 0) {
            setY(getY() - yspeed);
            facing = Direction.UP;
            isMoving = true;
        }
        if (activeKeys.contains(downKey) && getY() + size < screenHeight) {
            setY(getY() + yspeed);
            facing = Direction.DOWN;
            isMoving = true;
        }
        if (activeKeys.contains(leftKey) && getX() > 0) {
            setX(getX() - xspeed);
            facing = Direction.LEFT;
            isMoving = true;
        }
        if (activeKeys.contains(rightKey) && getX() + size < screenWidth) {
            setX(getX() + xspeed);
            facing = Direction.RIGHT;
            isMoving = true;
        }

        for (Rectangle wall : obstacles) {
            if (this.getBoundsInParent().intersects(wall.getBoundsInParent())) {
                setX(oldX);
                setY(oldY);
                break; 
            }
        }
        
        //animation computation
        if (isMoving) {
            animationTick++;
            
            // check which way we are facing to pick the right metronome speed
            int currentMetronome = animationSpeedX; 
            if (facing == Direction.UP || facing == Direction.DOWN) {
                currentMetronome = animationSpeedY;
            }

            if (animationTick >= currentMetronome) {
                animationTick = 0; 
                currentWalkFrame = (currentWalkFrame + 1) % 3; 
                updateAppearance();
            }
        } else {
            currentWalkFrame = 0;
            animationTick = 0;
        }

        if (wasMoving != isMoving || oldFacing != facing) {
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
        int dirIndex = 0; 
        if (facing == Direction.LEFT) dirIndex = 1;
        if (facing == Direction.RIGHT) dirIndex = 2;
        if (facing == Direction.UP) dirIndex = 3; 

        Image imageToDraw = null;

        if (hasBomb) {
            imageToDraw = isMoving ? bombWalkFrames[dirIndex][currentWalkFrame] : bombIdleFrames[dirIndex];
            // fallback: if bomb walk frame is missing, just show the idle bomb frame
            if (imageToDraw == null) imageToDraw = bombIdleFrames[dirIndex]; 
        } else {
            imageToDraw = isMoving ? walkFrames[dirIndex][currentWalkFrame] : idleFrames[dirIndex];
            // fallback: if walk frame is missing, show idle frame
            if (imageToDraw == null) imageToDraw = idleFrames[dirIndex];
        }
        
        this.setImage(imageToDraw);
    }
    
    public double getCenterX() { return getX() + (size / 2); }
    public double getCenterY() { return getY() + (size / 2); }
}