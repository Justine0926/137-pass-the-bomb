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
import java.util.function.BiConsumer;

public class MultiplayerMenu extends Pane {

    private VBox menuLayout;
    private Runnable onBack;
    private BiConsumer<Integer, String> onHost;
    private BiConsumer<String, String> onJoin;

    // Updated Constructor to expect the new data!
    public MultiplayerMenu(Runnable onBack, BiConsumer<Integer, String> onHost, BiConsumer<String, String> onJoin) {
        this.onBack = onBack;
        this.onHost = onHost;
        this.onJoin = onJoin;

        // Reduced spacing to fit the 400 height
        menuLayout = new VBox(20);
        menuLayout.setAlignment(Pos.CENTER);
        
        // Scaled to the new resolution
        menuLayout.setPrefSize(700, 400);
        menuLayout.setStyle("-fx-background-color: #111;");

        showMainSelection();

        this.getChildren().add(menuLayout);
    }

    // STATE 1: The Initial Buttons
    private void showMainSelection() {
        menuLayout.getChildren().clear();

        Text titleText = createTitle("LAN MULTIPLAYER");
        Button hostButton = createMenuButton("HOST NEW GAME");
        Button joinButton = createMenuButton("JOIN EXISTING GAME");
        Button backButton = createMenuButton("BACK TO MAIN MENU");

        hostButton.setOnAction(e -> showHostFields());
        joinButton.setOnAction(e -> showJoinFields());
        backButton.setOnAction(e -> onBack.run());

        menuLayout.getChildren().addAll(titleText, hostButton, joinButton, backButton);
    }

    // STATE 2: Host Game Fields
    private void showHostFields() {
        menuLayout.getChildren().clear();

        Text titleText = createTitle("HOST A GAME");
        
        TextField playersInput = createStyledTextField("Enter Number of Players (e.g. 3)");
        TextField nameInput = createStyledTextField("Enter Your Name");
        
        Button startServerBtn = createMenuButton("START SERVER");
        Button backButton = createMenuButton("CANCEL");

        startServerBtn.setOnAction(e -> {
            try {
                int maxPlayers = Integer.parseInt(playersInput.getText().trim());
                String name = nameInput.getText().trim();
                if (name.isEmpty() || maxPlayers < 2) return; // Basic validation
                
                onHost.accept(maxPlayers, name);
            } catch (NumberFormatException ex) {
                playersInput.setText("Invalid Number!");
            }
        });

        backButton.setOnAction(e -> showMainSelection());

        menuLayout.getChildren().addAll(titleText, playersInput, nameInput, startServerBtn, backButton);
    }

    // STATE 3: Join Game Fields
    private void showJoinFields() {
        menuLayout.getChildren().clear();

        Text titleText = createTitle("JOIN A GAME");
        
        TextField ipInput = createStyledTextField("Enter Host IP (e.g. 192.168.1.5)");
        TextField nameInput = createStyledTextField("Enter Your Name");
        
        Button connectBtn = createMenuButton("CONNECT");
        Button backButton = createMenuButton("CANCEL");

        connectBtn.setOnAction(e -> {
            String ip = ipInput.getText().trim();
            String name = nameInput.getText().trim();
            if (ip.isEmpty()) ip = "localhost";
            if (name.isEmpty()) return;

            onJoin.accept(ip, name);
        });

        backButton.setOnAction(e -> showMainSelection());

        menuLayout.getChildren().addAll(titleText, ipInput, nameInput, connectBtn, backButton);
    }

    // --- STYLING HELPERS ---
    private Text createTitle(String text) {
        Text title = new Text(text);
        // Scaled down from 60 to 35
        title.setFont(Font.font("Monospaced", FontWeight.BOLD, 35));
        title.setFill(Color.rgb(232, 224, 192));
        title.setStroke(Color.rgb(8, 8, 8));
        title.setStrokeWidth(2); // Thinner stroke
        
        DropShadow shadow = new DropShadow();
        shadow.setOffsetY(3); // Reduced shadow distance
        shadow.setOffsetX(3);
        shadow.setColor(Color.color(0, 0, 0, 0.8));
        title.setEffect(shadow);
        return title;
    }

    private TextField createStyledTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        // Scaled down font and width
        tf.setFont(Font.font("Monospaced", FontWeight.BOLD, 16));
        tf.setMaxWidth(300); 
        tf.setStyle("-fx-background-color: #222; -fx-text-fill: #E8E0C0; -fx-border-color: #555; -fx-border-width: 2; -fx-padding: 8; -fx-alignment: center;");
        return tf;
    }

    private Button createMenuButton(String text) {
        Button btn = new Button(text);
        // Scaled down from 30 to 20
        btn.setFont(Font.font("Monospaced", FontWeight.BOLD, 20));
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: rgb(144, 140, 128); -fx-cursor: hand;");
        
        DropShadow shadow = new DropShadow();
        shadow.setOffsetY(2); // Reduced shadow distance
        shadow.setOffsetX(2);
        shadow.setColor(Color.color(0, 0, 0, 0.8));
        btn.setEffect(shadow);

        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: rgb(232, 224, 192); -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: rgb(144, 140, 128); -fx-cursor: hand;"));
        return btn;
    }
}