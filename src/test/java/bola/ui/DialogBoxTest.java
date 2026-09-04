package bola.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
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

    @Test
    void userDialog_transparentAvatar_keepsOriginalBounds() throws Exception {
        FxTestSupport.run(() -> {
            DialogBox dialog = DialogBox.getUserDialog("list", new WritableImage(100, 100));
            ImageView picture = (ImageView) dialog.getChildren().getLast();
            assertEquals(null, picture.getViewport());
        });
    }

    @Test
    void userDialog_faintPixelsOutsideArtwork_ignoresExportArtifacts() throws Exception {
        FxTestSupport.run(() -> {
            WritableImage avatar = new WritableImage(100, 100);
            avatar.getPixelWriter().setArgb(0, 0, 0x01000000);
            avatar.getPixelWriter().setArgb(99, 99, 0x01000000);
            for (int y = 20; y < 80; y++) {
                for (int x = 10; x < 90; x++) {
                    avatar.getPixelWriter().setColor(x, y, Color.BROWN);
                }
            }
            DialogBox dialog = DialogBox.getUserDialog("okay", avatar);
            ImageView picture = (ImageView) dialog.getChildren().getLast();
            assertEquals(new Rectangle2D(10, 20, 80, 60), picture.getViewport());
        });
    }

    private void checkBubble(String message, boolean isUser) {
        Image avatar = new Image(getClass().getResourceAsStream(
                isUser ? "/images/DaUser.png" : "/images/DaBola.png"));
        DialogBox dialog = isUser ? DialogBox.getUserDialog(message, avatar)
                : DialogBox.getBolaDialog(message, avatar);
        VBox root = new VBox(dialog);
        new Scene(root, 380, 600);
        root.applyCss();
        root.layout();

        Label text = (Label) dialog.getChildren().get(isUser ? 0 : 1);
        ImageView picture = (ImageView) dialog.getChildren().get(isUser ? 1 : 0);
        assertTrue(picture.getViewport().getMinY() > 0, "The real avatars have transparent top padding");
        // Bounds of the visible artwork in the bundled PNGs, excluding faint export artifacts.
        Rectangle2D artwork = isUser ? new Rectangle2D(120, 197, 773, 625)
                : new Rectangle2D(145, 176, 826, 635);
        assertEquals(artwork, picture.getViewport());
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
            assertTrue(text.getBoundsInParent().getMaxY() > picture.getBoundsInParent().getMaxY());
            assertTrue(text.getHeight() >= text.prefHeight(text.getWidth()));
        }
    }

}
