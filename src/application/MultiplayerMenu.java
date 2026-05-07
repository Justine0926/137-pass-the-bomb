package application;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.function.Consumer;

public class MultiplayerMenu extends Pane {

    public MultiplayerMenu(Runnable onBack, Runnable onHost, Consumer<String> onJoin) {
        
        // --- TITLE TEXT ---
        Text titleText = new Text("LAN MULTIPLAYER");
        titleText.setFont(Font.font("Monospaced", FontWeight.BOLD, 60));
        titleText.setFill(Color.rgb(232, 224, 192)); // ACCENT color
        titleText.setStroke(Color.rgb(8, 8, 8));
        titleText.setStrokeWidth(3);

        DropShadow shadow = new DropShadow();
        shadow.setOffsetY(6);
        shadow.setOffsetX(6);
        shadow.setColor(Color.color(0, 0, 0, 0.8));
        titleText.setEffect(shadow);

        // --- IP INPUT BOX ---
        TextField ipInput = new TextField();
        ipInput.setPromptText("Enter Host IP (e.g., 192.168.1.5)");
        ipInput.setFont(Font.font("Monospaced", FontWeight.BOLD, 20));
        ipInput.setPrefWidth(400);
        ipInput.setStyle("-fx-background-color: #222; -fx-text-fill: #E8E0C0; -fx-border-color: #555; -fx-border-width: 2; -fx-padding: 10;");
        ipInput.setEffect(shadow);

        // --- BUTTONS ---
        Button hostButton = createMenuButton("HOST NEW GAME");
        Button joinButton = createMenuButton("JOIN EXISTING GAME");
        Button backButton = createMenuButton("BACK TO MAIN MENU");

        // --- BUTTON ACTIONS ---
        backButton.setOnAction(e -> onBack.run());
        
        hostButton.setOnAction(e -> {
            System.out.println("Starting Server...");
            onHost.run();
        });

        joinButton.setOnAction(e -> {
            String ip = ipInput.getText().trim();
            if (ip.isEmpty()) {
                ip = "localhost"; // Default to localhost if they leave it blank
            }
            System.out.println("Joining Server at: " + ip);
            onJoin.accept(ip);
        });

        // --- LAYOUT CONTAINER ---
        // VBox perfectly centers everything vertically
        VBox menuLayout = new VBox(30); // 30px spacing between items
        menuLayout.setAlignment(Pos.CENTER);
        menuLayout.setPrefSize(1400, 800); // Match your window size
        menuLayout.setStyle("-fx-background-color: #111;"); // Dark background

        menuLayout.getChildren().addAll(titleText, hostButton, ipInput, joinButton, backButton);

        // Add the VBox to this Pane
        this.getChildren().add(menuLayout);
    }

    // Helper method to create heavily styled buttons just like your GameBoard
    private Button createMenuButton(String text) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Monospaced", FontWeight.BOLD, 30));
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: rgb(144, 140, 128); -fx-cursor: hand;");
        
        DropShadow buttonShadow = new DropShadow();
        buttonShadow.setOffsetY(4);
        buttonShadow.setOffsetX(4);
        buttonShadow.setColor(Color.color(0, 0, 0, 0.8));
        btn.setEffect(buttonShadow);

        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: rgb(232, 224, 192); -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: rgb(144, 140, 128); -fx-cursor: hand;"));

        return btn;
    }
}