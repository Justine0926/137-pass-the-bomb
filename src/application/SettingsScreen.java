package application;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class SettingsScreen extends StackPane {
    private final Color BG_COLOR = Color.rgb(0, 0, 0, 0.85); 
    private final Color TEXT_COLOR = Color.rgb(0xE8, 0xE0, 0xC0);
    private final String FONT_FAMILY = "Monospaced";
    
    private boolean tempPowerUps;
    private boolean tempSfx;
    private boolean tempMusic;

    public SettingsScreen(Runnable onMenuReturn) {
        setBackground(new Background(new BackgroundFill(BG_COLOR, CornerRadii.EMPTY, Insets.EMPTY)));

        // Load current state into temporary variables
        tempPowerUps = GameSettings.powerUpsEnabled;
        tempSfx = GameSettings.sfxEnabled;
        tempMusic = GameSettings.musicEnabled;

        // SCALED: Reduced main spacing from 40 to 20
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        
        Label title = new Label("SETTINGS");
        // SCALED: Reduced title font from 48 to 36
        title.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 36));
        title.setTextFill(TEXT_COLOR);
        
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        // SCALED: Reduced grid gaps
        grid.setHgap(20);
        grid.setVgap(10);

        // 1. Round Duration
        Label durationLabel = new Label("Round Duration (sec):");
        // SCALED: Reduced label font from 24 to 16
        durationLabel.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 16));
        durationLabel.setTextFill(TEXT_COLOR);
        
        TextField durationInput = new TextField(String.valueOf(GameSettings.roundDurationSeconds));
        // SCALED: Reduced input font from 24 to 16 and width from 80px to 60px
        durationInput.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 16));
        durationInput.setStyle("-fx-background-color: transparent; -fx-text-fill: #e8e0c0; -fx-border-color: #e8e0c0; -fx-border-width: 0 0 2px 0; -fx-pref-width: 60px; -fx-alignment: center;");
        
        // Restrict to numbers only
        durationInput.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                durationInput.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });

        // 2. Power-ups Toggle
        Label powerUpsLabel = new Label("Power-ups:");
        powerUpsLabel.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 16));
        powerUpsLabel.setTextFill(TEXT_COLOR);
        Button powerUpsBtn = createToggleButton(tempPowerUps, v -> tempPowerUps = v);

        // 3. SFX Toggle
        Label sfxLabel = new Label("Sound Effects:");
        sfxLabel.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 16));
        sfxLabel.setTextFill(TEXT_COLOR);
        Button sfxBtn = createToggleButton(tempSfx, v -> tempSfx = v);

        // 4. Music Toggle
        Label musicLabel = new Label("Music:");
        musicLabel.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 16));
        musicLabel.setTextFill(TEXT_COLOR);
        Button musicBtn = createToggleButton(tempMusic, v -> tempMusic = v);

        grid.add(durationLabel, 0, 0);
        grid.add(durationInput, 1, 0);
        
        grid.add(powerUpsLabel, 0, 1);
        grid.add(powerUpsBtn, 1, 1);
        
        grid.add(sfxLabel, 0, 2);
        grid.add(sfxBtn, 1, 2);
        
        grid.add(musicLabel, 0, 3);
        grid.add(musicBtn, 1, 3);

        // Save & Cancel Buttons
        Button saveBtn = new Button("SAVE");
        styleButton(saveBtn);
        saveBtn.setOnAction(e -> {
            int newDur = 90;
            try {
                if (!durationInput.getText().isEmpty()) {
                    newDur = Integer.parseInt(durationInput.getText());
                    if (newDur <= 0) newDur = 10; // minimum duration
                }
            } catch (Exception ex) {}
            
            GameSettings.roundDurationSeconds = newDur;
            GameSettings.powerUpsEnabled = tempPowerUps;
            GameSettings.sfxEnabled = tempSfx;
            GameSettings.musicEnabled = tempMusic;
            
            if (GameSettings.onSettingsUpdated != null) {
                GameSettings.onSettingsUpdated.run();
            }
            onMenuReturn.run();
        });

        Button cancelBtn = new Button("CANCEL");
        styleButton(cancelBtn);
        cancelBtn.setOnAction(e -> onMenuReturn.run());

        // SCALED: Reduced HBox spacing to 20 and top padding to 15
        HBox btnBox = new HBox(20, saveBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER);
        btnBox.setPadding(new Insets(15, 0, 0, 0));

        layout.getChildren().addAll(title, grid, btnBox);
        getChildren().add(layout);
    }
    
    // Helper to create an ON/OFF toggle button
    private Button createToggleButton(boolean initialState, java.util.function.Consumer<Boolean> onToggle) {
        Button btn = new Button(initialState ? "ON" : "OFF");
        // SCALED: Reduced font from 24 to 16
        btn.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 16));
        // Use an array to hold mutable state inside lambda
        final boolean[] state = { initialState };
        
        Runnable updateStyle = () -> {
            // SCALED: Reduced button width from 100px to 70px
            if (state[0]) {
                btn.setStyle("-fx-background-color: #e8e0c0; -fx-text-fill: #1b1b3a; -fx-border-color: #e8e0c0; -fx-border-width: 2px; -fx-pref-width: 70px;");
            } else {
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e8e0c0; -fx-border-color: #e8e0c0; -fx-border-width: 2px; -fx-pref-width: 70px;");
            }
            btn.setText(state[0] ? "ON" : "OFF");
        };
        updateStyle.run();
        
        btn.setOnAction(e -> {
            state[0] = !state[0];
            onToggle.accept(state[0]);
            updateStyle.run();
        });
        
        return btn;
    }

    private void styleButton(Button btn) {
        // SCALED: Reduced font from 24 to 16 and width from 150px to 120px
        btn.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 16));
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e8e0c0; -fx-border-color: #e8e0c0; -fx-border-width: 2px; -fx-pref-width: 120px;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #e8e0c0; -fx-text-fill: #1b1b3a; -fx-pref-width: 120px;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e8e0c0; -fx-border-color: #e8e0c0; -fx-border-width: 2px; -fx-pref-width: 120px;"));
    }
}