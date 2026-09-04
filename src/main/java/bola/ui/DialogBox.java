package bola.ui;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

/**
 * Controls an FXML chat bubble and its speaker's avatar.
 */
public class DialogBox extends HBox {
    /** Ignores nearly invisible export artifacts when locating the avatar artwork. */
    private static final int MIN_VISIBLE_ALPHA = 8;

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
        trimAvatarPadding(image);
    }

    /**
     * Removes transparent and nearly invisible margins so alignment follows the visible artwork.
     */
    private void trimAvatarPadding(Image image) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        int left = width;
        int top = height;
        int right = -1;
        int bottom = -1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if ((image.getPixelReader().getArgb(x, y) >>> 24) > MIN_VISIBLE_ALPHA) {
                    left = Math.min(left, x);
                    top = Math.min(top, y);
                    right = Math.max(right, x);
                    bottom = Math.max(bottom, y);
                }
            }
        }
        if (right >= left && bottom >= top) {
            displayPicture.setViewport(new Rectangle2D(left, top, right - left + 1, bottom - top + 1));
        }
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
