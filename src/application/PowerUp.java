package application;

import javafx.geometry.Bounds;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

// Represents a collectible power-up item on the game board.
public class PowerUp {

    public enum PowerUpType {
        SPEED,
        SHIELD
    }

    private final PowerUpType type;
    private final ImageView sprite;
    private boolean collected = false;

    private static final double SIZE = 60;

    public PowerUp(PowerUpType type, double x, double y) {
        this.type = type;

        // pick the right sprite based on type
        String spritePath = switch (type) {
            case SPEED  -> "/sprites/powerups/speed.png";
            case SHIELD -> "/sprites/powerups/shield.png";
        };

        Image img = safeLoad(spritePath);
        sprite = new ImageView(img);
        sprite.setFitWidth(SIZE);
        sprite.setFitHeight(SIZE);
        sprite.setPreserveRatio(true);
        sprite.setSmooth(false);
        sprite.setX(x);
        sprite.setY(y);
    }

    // load image safely — returns null image view if file is missing
    private Image safeLoad(String path) {
        java.net.URL url = getClass().getResource(path);
        if (url == null) {
            System.err.println("WARNING: PowerUp sprite not found -> " + path);
            return null;
        }
        return new Image(url.toExternalForm());
    }

    public PowerUpType getType() {
        return type;
    }

    public ImageView getSprite() {
        return sprite;
    }

    // returns the visual/collision bounds of the sprite in the parent container
    public Bounds getBounds() {
        return sprite.getBoundsInParent();
    }

    public boolean isCollected() {
        return collected;
    }

    // marks this power-up as collected and hides the sprite
    public void collect() {
        collected = true;
        sprite.setVisible(false);
    }
}
