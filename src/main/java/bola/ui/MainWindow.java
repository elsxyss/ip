package bola.ui;

import bola.Bola;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Handles chat input and the closing sequence for the main FXML view.
 */
public class MainWindow {
    static final String CLOSING_NOTICE = "[Closing in 5 seconds...]";

    @FXML
    private ScrollPane scrollPane;
    @FXML
    private VBox dialogContainer;
    @FXML
    private TextField userInput;
    @FXML
    private Button sendButton;
    @FXML
    private Image userImage;
    @FXML
    private Image bolaImage;

    private Bola bola;
    private Timeline closingTimeline;

    /**
     * Connects automatic scrolling after FXML has injected the controls.
     */
    @FXML
    private void initialize() {
        dialogContainer.heightProperty().addListener((observable) -> scrollPane.setVvalue(1.0));
    }

    /**
     * Connects the chatbot and displays its initial greeting.
     */
    public void setBola(Bola bola) {
        this.bola = bola;
        dialogContainer.getChildren().add(DialogBox.getBolaDialog(bola.getWelcome(), bolaImage));
    }

    /**
     * Focuses the command field once the window is visible.
     */
    public void focusInput() {
        userInput.requestFocus();
    }

    /**
     * Adds both sides of the exchange and starts an eight-second closing sequence after bye.
     */
    @FXML
    private void handleUserInput() {
        String input = userInput.getText();
        if (input.isBlank() || bola.isExit()) {
            return;
        }
        String response = bola.getResponse(input);
        dialogContainer.getChildren().addAll(
                DialogBox.getUserDialog(input, userImage),
                DialogBox.getBolaDialog(response, bolaImage));
        userInput.clear();
        userInput.setDisable(bola.isExit());
        sendButton.setDisable(bola.isExit());
        if (bola.isExit()) {
            closingTimeline = createClosingTimeline(
                    () -> dialogContainer.getChildren().add(
                            DialogBox.getBolaDialog(CLOSING_NOTICE, bolaImage)),
                    Platform::exit);
            closingTimeline.play();
        } else {
            userInput.requestFocus();
        }
    }

    /**
     * Schedules a separate notice after three seconds and closes after eight, without blocking the UI.
     */
    static Timeline createClosingTimeline(Runnable showCountdown, Runnable closeWindow) {
        return new Timeline(
                new KeyFrame(Duration.seconds(3), event -> showCountdown.run()),
                new KeyFrame(Duration.seconds(8), event -> closeWindow.run()));
    }

    /**
     * Cancels pending countdown callbacks when the application stops.
     */
    public void stop() {
        if (closingTimeline != null) {
            closingTimeline.stop();
        }
    }
}
