package application;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

//THIS IS THE CLASS FOR THE MAIN MENU SCREEN
//it handles the start game, and exit functions
public class MainMenu extends VBox {
    
    // we pass in 'Runnables' (actions) so the menu knows what to do when clicked
    public MainMenu(Runnable onStart, Runnable onExit) {
        // center everything and add space between items
        this.setAlignment(Pos.CENTER);
        this.setSpacing(30); 

        // create the Title
        Text title = new Text("Pass The Bomb");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 50));

        // create the Start Button
        Button startBtn = new Button("Start Game");
        startBtn.setFont(Font.font("Arial", 24));
        startBtn.setPrefWidth(200);
        startBtn.setOnAction(e -> onStart.run());

        // create the Exit Button
        Button exitBtn = new Button("Exit");
        exitBtn.setFont(Font.font("Arial", 24));
        exitBtn.setPrefWidth(200);
        exitBtn.setOnAction(e -> onExit.run());

        // add them all to the screen
        this.getChildren().addAll(title, startBtn, exitBtn);
    }
}