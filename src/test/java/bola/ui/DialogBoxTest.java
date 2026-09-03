package bola.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Checks bubble appearance and actual layout after JavaFX applies its styles.
 */
public class DialogBoxTest {
    @BeforeAll
    static void startJavaFx() throws Exception {
        FxTestSupport.start();
    }

    @Test
    void userDialog_shortMessage_hasSquareBottomRightAndCompactHeight() throws Exception {
        FxTestSupport.run(() -> checkBubble("list", true));
    }

    @Test
    void bolaDialog_shortMessage_hasSquareBottomLeftAndCompactHeight() throws Exception {
        FxTestSupport.run(() -> checkBubble("Hello!", false));
    }

    @Test
    void bothDialogs_longMessage_wrapWithinRowAndStayTopAligned() throws Exception {
        FxTestSupport.run(() -> {
            String message = "A long task description with several words that must wrap. ".repeat(8)
                    + "\nA second line.";
            checkBubble(message, true);
            checkBubble(message, false);
        });
    }

    private void checkBubble(String message, boolean isUser) {
        WritableImage avatar = new WritableImage(100, 100);
        DialogBox dialog = isUser ? DialogBox.getUserDialog(message, avatar)
                : DialogBox.getBolaDialog(message, avatar);
        VBox root = new VBox(dialog);
        new Scene(root, 380, 600);
        root.applyCss();
        root.layout();

        Label text = (Label) dialog.getChildren().get(isUser ? 0 : 1);
        ImageView picture = (ImageView) dialog.getChildren().get(isUser ? 1 : 0);
        assertEquals(message, text.getText());
        assertEquals(Color.WHITE, text.getBackground().getFills().getFirst().getFill());
        assertEquals(Color.BLACK, text.getBorder().getStrokes().getFirst().getTopStroke());
        assertEquals(Color.BLACK, text.getTextFill());
        CornerRadii corners = text.getBorder().getStrokes().getFirst().getRadii();
        assertEquals(16, corners.getTopLeftHorizontalRadius());
        assertEquals(16, corners.getTopRightHorizontalRadius());
        assertEquals(isUser ? 0 : 16, corners.getBottomRightHorizontalRadius());
        assertEquals(isUser ? 16 : 0, corners.getBottomLeftHorizontalRadius());
        assertEquals(corners, text.getBackground().getFills().getFirst().getRadii());
        assertEquals(picture.getBoundsInParent().getMinY(), text.getBoundsInParent().getMinY());
        assertTrue(text.getBoundsInParent().getMinX() >= 0);
        assertTrue(text.getBoundsInParent().getMaxX() <= dialog.getWidth());
        assertTrue(text.getWidth() + picture.getBoundsInParent().getWidth()
                + dialog.getSpacing() <= dialog.getWidth());
        if (message.length() < 20) {
            assertTrue(text.getHeight() < picture.getBoundsInParent().getHeight());
        } else {
            assertTrue(text.isWrapText());
            assertTrue(text.getHeight() > picture.getBoundsInParent().getHeight());
            assertTrue(text.getHeight() >= text.prefHeight(text.getWidth()));
        }
    }

}
