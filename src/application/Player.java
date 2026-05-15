package application;

import java.util.List;
import java.util.Set;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class Player extends ImageView {
    
	//enum for directions
    public enum Direction { DOWN, LEFT, RIGHT, UP }
    
    private boolean hasBomb;
    private boolean isMoving = false;
    private boolean isFrozen = false;
    private Direction facing = Direction.DOWN; 
    
    private static final int BASE_XSPEED = 5;
    private static final int BASE_YSPEED = 4;
    private int xspeed = BASE_XSPEED;
    private int yspeed = BASE_YSPEED;
    private final double size = 60; 
    
    // power-up state
    private boolean shielded = false;
    private final ImageView shieldAura;
    private final double shieldAuraSize = 90; // on-screen diameter of the pixel-art bubble
    private final DropShadow speedGlow;
    
    // arrays for images (0=Down, 1=Left, 2=Right, 3=Up)
    private final Image[] idleFrames = new Image[4];
    private final Image[] bombIdleFrames = new Image[4];
    private final Image[][] walkFrames = new Image[4][3];
    private final Image[][] bombWalkFrames = new Image[4][3];
    private final Image[] frozenFrames = new Image[4];
    private final Image[] frozenBombFrames = new Image[4];
    
    private int currentWalkFrame = 0;
    private int animationTick = 0;
    private final int animationSpeedX = 12; // animation for Left/Right
    private final int animationSpeedY = 10; // animation for Up/Down
    private final Text nameTag; //name
    
    private final KeyCode upKey, downKey, leftKey, rightKey;

    public Player(double startX, double startY, 
                  KeyCode up, KeyCode down, KeyCode left, KeyCode right, boolean startsWithBomb, String playerName) {
        
        // shield aura: pixel-art cyan bubble drawn around the player when shielded
        Image auraImg = safeLoad("/sprites/powerups/shield_aura.png");
        shieldAura = new ImageView(auraImg);
        shieldAura.setFitWidth(shieldAuraSize);
        shieldAura.setFitHeight(shieldAuraSize);
        shieldAura.setPreserveRatio(true);
        shieldAura.setSmooth(false);          // keep crisp pixels
        shieldAura.setMouseTransparent(true);
        shieldAura.setVisible(false);
        
        // speed glow effect: yellow/orange drop shadow applied to the sprite while boosted
        speedGlow = new DropShadow();
        speedGlow.setColor(Color.color(1.0, 0.85, 0.2, 0.9));
        speedGlow.setRadius(25);
        speedGlow.setSpread(0.4);
        
    	//name field
    	nameTag = new Text(playerName);
        nameTag.setFont(Font.font("Monospaced", FontWeight.BOLD, 14));
        nameTag.setFill(Color.WHITE);
        DropShadow tagShadow = new DropShadow();
        tagShadow.setOffsetY(2);
        tagShadow.setOffsetX(2);
        tagShadow.setColor(Color.BLACK);
        tagShadow.setRadius(0);
        nameTag.setEffect(tagShadow);
    	
        String[] dirs = {"down", "left", "right", "up"};
        
        for (int d = 0; d < 4; d++) {
            String dir = dirs[d];
            
            // load Idle Images
            // matches: /idle/idle_down.png
            idleFrames[d] = safeLoad("/sprites/idle/idle_" + dir + ".png");
            
            // matches: /idle_bomb/bomb_idle_down.png
            bombIdleFrames[d] = safeLoad("/sprites/idle_bomb/bomb_idle_" + dir + ".png");
            
            // load Frozen Images
            frozenFrames[d] = safeLoad("/sprites/freeze/frozen_" + 
                (dir.equals("up") ? "back" : dir.equals("down") ? "front" : dir) + ".png");
                
            // load Frozen Bomb Images
            frozenBombFrames[d] = safeLoad("/sprites/freeze/frozen_bomb_" + 
                (dir.equals("up") ? "back" : dir.equals("down") ? "front" : dir) + ".png");
            
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
        updateNameTagPosition();
        updateShieldAuraPosition();
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

        if (isFrozen) {
            updateNameTagPosition();
            return;
        }

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
        updateNameTagPosition();
        updateShieldAuraPosition();
    }

    public boolean hasBomb() {
        return hasBomb;
    }

    public void setBomb(boolean hasBomb) {
        this.hasBomb = hasBomb;
        updateAppearance();
    }

    public boolean isFrozen() {
        return isFrozen;
    }

    public void setFrozen(boolean isFrozen) {
        this.isFrozen = isFrozen;
        updateAppearance();
    }

    private void updateAppearance() {
        int dirIndex = 0; 
        if (facing == Direction.LEFT) dirIndex = 1;
        if (facing == Direction.RIGHT) dirIndex = 2;
        if (facing == Direction.UP) dirIndex = 3; 

        Image imageToDraw = null;

        if (isFrozen) {
            if (hasBomb) {
                imageToDraw = frozenBombFrames[dirIndex];
                if (imageToDraw == null) imageToDraw = bombIdleFrames[dirIndex];
            } else {
                imageToDraw = frozenFrames[dirIndex];
                if (imageToDraw == null) imageToDraw = idleFrames[dirIndex];
            }
        } else if (hasBomb) {
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
    private void updateShieldAuraPosition() {
        // center the aura on the rendered sprite bounds (preserveRatio can
        // shrink actual on-screen width/height below the nominal `size`).
        javafx.geometry.Bounds b = this.getBoundsInParent();
        double cx = b.getMinX() + b.getWidth()  / 2.0;
        double cy = b.getMinY() + b.getHeight() / 2.0;
        shieldAura.setX(cx - shieldAuraSize / 2.0);
        shieldAura.setY(cy - shieldAuraSize / 2.0);
    }
    
    // --- Power-Up Methods ---
    
    public void applySpeedBoost(double multiplier) {
        xspeed = (int) Math.round(BASE_XSPEED * multiplier);
        yspeed = (int) Math.round(BASE_YSPEED * multiplier);
        setEffect(speedGlow);
    }
    
    public void resetSpeed() {
        xspeed = BASE_XSPEED;
        yspeed = BASE_YSPEED;
        if (getEffect() == speedGlow) {
            setEffect(null);
        }
    }
    
    public boolean isShielded() {
        return shielded;
    }
    
    public void setShielded(boolean v) {
        shielded = v;
        // reset any leftover transform/opacity from a previous break animation
        shieldAura.setScaleX(1.0);
        shieldAura.setScaleY(1.0);
        shieldAura.setOpacity(1.0);
        shieldAura.setVisible(v);
    }
    
    public Node getShieldAura() {
        return shieldAura;
    }
    
    // plays a quick pop+fade animation, then disables shield
    public void breakShield() {
        if (!shielded) return;
        shielded = false; // logically broken immediately so no further blocks happen
        
        ScaleTransition pop = new ScaleTransition(Duration.millis(180), shieldAura);
        pop.setFromX(1.0); pop.setFromY(1.0);
        pop.setToX(1.6);   pop.setToY(1.6);
        
        FadeTransition fade = new FadeTransition(Duration.millis(180), shieldAura);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        
        ParallelTransition pt = new ParallelTransition(pop, fade);
        pt.setOnFinished(e -> {
            shieldAura.setVisible(false);
            shieldAura.setScaleX(1.0);
            shieldAura.setScaleY(1.0);
            shieldAura.setOpacity(1.0);
        });
        pt.play();
    }
    
    private void updateNameTagPosition() {
        //actual width of the text
        double textWidth = nameTag.getLayoutBounds().getWidth();
        
        // actual rendered width of the player sprite
        double playerWidth = this.getBoundsInParent().getWidth(); 
        
        // center the text 
        nameTag.setX(getX() + (playerWidth / 2) - (textWidth / 2));
        
        // float it 5 pixels above the character's head
        nameTag.setY(getY() - 5); 
    }
    
    public double getCenterX() { return getX() + (size / 2); }
    public double getCenterY() { return getY() + (size / 2); }
    public Text getNameTag() { return nameTag; }
    
    public Direction getFacing() {
        return facing;
    }
    public void updateNetworkAnimation(String facingStr, boolean networkIsMoving) {
		// 1. Update the facing direction from the network string
		try {
			this.facing = Direction.valueOf(facingStr); 
		} catch (IllegalArgumentException e) {
			// Safety catch just in case the network sends garbage
			System.err.println("Unknown direction string: " + facingStr);
		}

		// 2. Set the moving state
		this.isMoving = networkIsMoving;

		// 3. Process the animation frames using your exact same metronome logic
		if (this.isMoving) {
			animationTick++;

			int currentMetronome = animationSpeedX; 
			if (facing == Direction.UP || facing == Direction.DOWN) {
				currentMetronome = animationSpeedY;
			}

			if (animationTick >= currentMetronome) {
				animationTick = 0; 
				currentWalkFrame = (currentWalkFrame + 1) % 3; 
			}
		} else {
			// Reset to idle standing frame
			currentWalkFrame = 0;
			animationTick = 0;
		}

		// 4. Force the image and nametag to visually update!
		updateAppearance();
		updateNameTagPosition();
	}
}
