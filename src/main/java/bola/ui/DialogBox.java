package bola.ui;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

/**
 * Controls an FXML chat bubble and its speaker's avatar.
 */
public class DialogBox extends HBox {
    @FXML
    private Label text;
    @FXML
    private ImageView displayPicture;

    private DialogBox(String message, Image image) {
        FXMLLoader loader = new FXMLLoader(DialogBox.class.getResource("/view/DialogBox.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new IllegalStateException("Could not load the chat bubble layout.", e);
        }
        text.setText(message);
        displayPicture.setImage(image);
    }

    /**
     * Returns a user message with the avatar on the right.
     */
    public static DialogBox getUserDialog(String message, Image image) {
        return new DialogBox(message, image);
    }

    /**
     * Returns a Bola message with the avatar on the left.
     */
    public static DialogBox getBolaDialog(String message, Image image) {
        DialogBox dialog = new DialogBox(message, image);
        dialog.flip();
        return dialog;
    }

    /**
     * Moves the avatar before the text and aligns the reply to the left.
     */
    private void flip() {
        setAlignment(Pos.TOP_LEFT);
        getStyleClass().add("bola-dialog");
        getChildren().addFirst(getChildren().removeLast());
    }

}
