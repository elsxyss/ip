package bola.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import bola.Bola;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Tests FXML loading, command handlers, and the closing sequence with isolated storage.
 */
public class MainWindowTest {
    @TempDir
    Path temporaryDirectory;

    @BeforeAll
    static void startJavaFx() throws Exception {
        FxTestSupport.start();
    }

    @Test
    void mainWindow_fxmlHandlers_preserveChatBehavior() throws Exception {
        FxTestSupport.run(() -> {
            FXMLLoader loader = new FXMLLoader(MainWindow.class.getResource("/view/MainWindow.fxml"));
            AnchorPane root = loader.load();
            MainWindow controller = loader.getController();
            Path storage = temporaryDirectory.resolve("bola.txt");
            controller.setBola(new Bola(storage.toString()));
            new Scene(root);
            root.applyCss();
            root.layout();

            TextField input = (TextField) loader.getNamespace().get("userInput");
            Button send = (Button) loader.getNamespace().get("sendButton");
            VBox dialogs = (VBox) loader.getNamespace().get("dialogContainer");
            ScrollPane scroll = (ScrollPane) loader.getNamespace().get("scrollPane");
            assertEquals(400, root.getPrefWidth());
            assertEquals(600, root.getPrefHeight());
            assertTrue(scroll.isFitToWidth());
            assertEquals(ScrollPane.ScrollBarPolicy.NEVER, scroll.getHbarPolicy());
            assertEquals(1, dialogs.getChildren().size());
            assertEquals("Bola: Eh hello! I'm Bola.\nGot anything to settle today?", messageAt(dialogs, 0));
            ImageView bolaPicture = (ImageView) ((DialogBox) dialogs.getChildren().getFirst())
                    .getChildren().getFirst();
            assertFalse(bolaPicture.getImage().isError());

            input.setText("   ");
            input.fireEvent(new ActionEvent());
            assertEquals(1, dialogs.getChildren().size());

            input.setText("todo read book");
            input.fireEvent(new ActionEvent());
            assertEquals(3, dialogs.getChildren().size());
            assertEquals("todo read book", messageAt(dialogs, 1));
            assertTrue(messageAt(dialogs, 2).contains("[T][ ] read book"));
            assertEquals("", input.getText());
            assertTrue(Files.readString(storage).contains("read book"));
            ImageView userPicture = (ImageView) ((DialogBox) dialogs.getChildren().get(1))
                    .getChildren().getLast();
            assertFalse(userPicture.getImage().isError());
            assertTrue(userPicture.getImage().getUrl().endsWith("DaUser.png"));
            assertTrue(bolaPicture.getImage().getUrl().endsWith("DaBola.png"));

            input.setText("list");
            send.fire();
            assertEquals(5, dialogs.getChildren().size());
            assertTrue(messageAt(dialogs, 4).contains("1. [T][ ] read book"));
            assertEquals("", input.getText());
            root.applyCss();
            root.layout();
            assertEquals(1.0, scroll.getVvalue());

            input.setText("bye");
            send.fire();
            try {
                assertEquals(7, dialogs.getChildren().size());
                assertTrue(messageAt(dialogs, 6).contains("All settled?"));
                assertTrue(input.isDisabled());
                assertTrue(send.isDisabled());
                input.setText("list");
                input.fireEvent(new ActionEvent());
                assertEquals(7, dialogs.getChildren().size());
            } finally {
                controller.stop();
            }
            return null;
        });
    }

    private String messageAt(VBox dialogs, int index) {
        DialogBox dialog = (DialogBox) dialogs.getChildren().get(index);
        return dialog.getChildren().stream().filter(Label.class::isInstance)
                .map(Label.class::cast).findFirst().orElseThrow().getText();
    }

    @Test
    void closingNotice_matchesRequestedText() {
        assertEquals("[Closing in 5 seconds...]", MainWindow.CLOSING_NOTICE);
    }

    @Test
    void createClosingTimeline_eightSeconds_countsDownBeforeClosing() {
        List<String> events = new ArrayList<>();
        Timeline timeline = MainWindow.createClosingTimeline(
                () -> events.add("countdown"), () -> events.add("close"));

        assertTrue(events.isEmpty());
        assertEquals(1, timeline.getCycleCount());
        assertEquals(2, timeline.getKeyFrames().size());
        assertEquals(Duration.seconds(3), timeline.getKeyFrames().get(0).getTime());
        assertEquals(Duration.seconds(8), timeline.getKeyFrames().get(1).getTime());
        assertEquals(Duration.seconds(5), timeline.getKeyFrames().get(1).getTime()
                .subtract(timeline.getKeyFrames().get(0).getTime()));
        assertEquals(Duration.seconds(8), timeline.getTotalDuration());

        timeline.getKeyFrames().get(0).getOnFinished().handle(new ActionEvent());
        assertEquals(List.of("countdown"), events);
        timeline.getKeyFrames().get(1).getOnFinished().handle(new ActionEvent());
        assertEquals(List.of("countdown", "close"), events);
    }
}
