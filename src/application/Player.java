package application;

import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.Set;

public class Player extends Circle {
    private boolean hasBomb;
    private final Color baseColor;
    private final int speed = 6;
    
    // Keybinds for this specific player
    private final KeyCode upKey, downKey, leftKey, rightKey;

    public Player(double startX, double startY, Color baseColor, 
                  KeyCode up, KeyCode down, KeyCode left, KeyCode right, boolean startsWithBomb) {
        super(startX, startY, 20); // 20 is the radius of the player
        this.baseColor = baseColor;
        this.upKey = up;
        this.downKey = down;
        this.leftKey = left;
        this.rightKey = right;
        this.hasBomb = startsWithBomb;
        updateAppearance();
    }

    // Move based on which keys are currently being pressed
    public void move(Set<KeyCode> activeKeys, double screenWidth, double screenHeight) {
        if (activeKeys.contains(upKey) && getCenterY() - getRadius() > 0) {
            setCenterY(getCenterY() - speed);
        }
        if (activeKeys.contains(downKey) && getCenterY() + getRadius() < screenHeight) {
            setCenterY(getCenterY() + speed);
        }
        if (activeKeys.contains(leftKey) && getCenterX() - getRadius() > 0) {
            setCenterX(getCenterX() - speed);
        }
        if (activeKeys.contains(rightKey) && getCenterX() + getRadius() < screenWidth) {
            setCenterX(getCenterX() + speed);
        }
    }

    public boolean hasBomb() {
        return hasBomb;
    }

    public void setBomb(boolean hasBomb) {
        this.hasBomb = hasBomb;
        updateAppearance();
    }

    // Turns the player red if they have the bomb, otherwise their normal color
    private void updateAppearance() {
        if (hasBomb) {
            this.setFill(Color.RED);
            this.setStroke(Color.DARKRED);
            this.setStrokeWidth(3);
        } else {
            this.setFill(baseColor);
            this.setStroke(Color.TRANSPARENT);
        }
    }
}