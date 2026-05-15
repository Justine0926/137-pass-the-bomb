package application;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class HowToPlayScreen extends StackPane {
    
    private final Color BG_COLOR = Color.rgb(0, 0, 0, 0.85); // Semi-transparent black 
    private final Color TEXT_COLOR = Color.rgb(0xE8, 0xE0, 0xC0);
    private final String FONT_FAMILY = "Monospaced";

    public HowToPlayScreen(Runnable onMenuReturn) {
        setBackground(new Background(new BackgroundFill(BG_COLOR, CornerRadii.EMPTY, Insets.EMPTY)));

        VBox content = new VBox(5);
        // SCALED: Reduced side padding from 80 to 40 so text has more room to breathe on a small screen
        content.setPadding(new Insets(10, 40, 10, 40)); 
        content.setAlignment(Pos.TOP_LEFT);
        content.setBackground(Background.EMPTY);

        // Title
        Label title = new Label("How to Play Pass the Bomb");
        // SCALED: Reduced title font from 48 to 32
        title.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 32));
        title.setTextFill(TEXT_COLOR);
        
        // --- Sections ---
        addHeading(content, "Objective");
        addText(content, "Avoid holding the bomb when the timer runs out.");
        
        addHeading(content, "Gameplay");
        addBullet(content, "The bomb is randomly assigned to one player at the start of the round.");
        addBullet(content, "To pass the bomb, collide or touch another player.");
        addBullet(content, "The bomb automatically transfers to the player you collide with.");
        addBullet(content, "The timer continuously counts down during the round.");
        addBullet(content, "When the timer reaches zero, the player holding the bomb explodes and loses.");
        addBullet(content, "The last remaining player wins the game.");

        addHeading(content, "Controls");
        addBullet(content, "Move – Use the movement keys to run around the map.");
        addBullet(content, "The bomb is passed automatically through player collision.");

        addHeading(content, "Power-Ups");
        addSubHeading(content, "❄️ Freeze");
        addBullet(content, "Temporarily freezes another player.");
        addBullet(content, "Frozen players cannot move for a short duration.");
        
        addSubHeading(content, "⚡ Speed");
        addBullet(content, "Increases movement speed temporarily.");
        addBullet(content, "Useful for escaping players or chasing someone with the bomb.");
        
        addSubHeading(content, "🛡️ Shield");
        addBullet(content, "Grants temporary protection from negative effects.");
        addBullet(content, "Can help prevent being affected by certain power-ups.");

        addHeading(content, "Tips");
        addBullet(content, "Keep moving to avoid getting trapped.");
        addBullet(content, "Use speed boosts to escape dangerous situations.");
        addBullet(content, "Time your freeze power-up carefully.");
        addBullet(content, "Stay away from players holding the bomb near the end of the timer.");

        Button backBtn = new Button("BACK TO MAIN MENU");
        // SCALED: Reduced button font from 24 to 16
        backBtn.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 16));
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e8e0c0; -fx-border-color: #e8e0c0; -fx-border-width: 2px;");
        backBtn.setOnMouseEntered(e -> backBtn.setStyle("-fx-background-color: #e8e0c0; -fx-text-fill: #1b1b3a;"));
        backBtn.setOnMouseExited(e -> backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e8e0c0; -fx-border-color: #e8e0c0; -fx-border-width: 2px;"));
        backBtn.setOnAction(e -> onMenuReturn.run());

        // SCALED: Reduced VBox spacing from 30 to 15
        VBox layout = new VBox(15);
        layout.setAlignment(Pos.TOP_CENTER);
        // SCALED: Reduced overall padding from 40 to 20
        layout.setPadding(new Insets(20));
        layout.setBackground(Background.EMPTY);
        
        layout.getChildren().addAll(title, content, backBtn);

        ScrollPane scrollPane = new ScrollPane(layout);
        scrollPane.setFitToWidth(true);
        // Style the ScrollPane to remove the default white background
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");

        getChildren().add(scrollPane);
    }

    private void addHeading(VBox container, String text) {
        Label heading = new Label(text);
        // SCALED: Reduced heading font from 32 to 22
        heading.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 22));
        heading.setTextFill(TEXT_COLOR);
        // SCALED: Reduced margins
        VBox.setMargin(heading, new Insets(15, 0, 5, 0));
        container.getChildren().add(heading);
    }
    
    private void addSubHeading(VBox container, String text) {
        Label heading = new Label(text);
        // SCALED: Reduced subheading font from 24 to 18
        heading.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 18));
        heading.setTextFill(TEXT_COLOR);
        // SCALED: Reduced margins
        VBox.setMargin(heading, new Insets(10, 0, 5, 0));
        container.getChildren().add(heading);
    }

    private void addText(VBox container, String text) {
        Label label = new Label(text);
        // SCALED: Reduced text font from 20 to 14
        label.setFont(Font.font(FONT_FAMILY, FontWeight.NORMAL, 14));
        label.setTextFill(TEXT_COLOR);
        label.setWrapText(true);
        container.getChildren().add(label);
    }

    private void addBullet(VBox container, String text) {
        Label label = new Label("• " + text);
        // SCALED: Reduced bullet font from 20 to 14
        label.setFont(Font.font(FONT_FAMILY, FontWeight.NORMAL, 14));
        label.setTextFill(TEXT_COLOR);
        label.setWrapText(true);
        // SCALED: Adjusted bullet indent margins
        VBox.setMargin(label, new Insets(0, 0, 4, 15)); 
        container.getChildren().add(label);
    }
}