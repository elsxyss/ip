package bola.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import bola.Bola;
import bola.Main;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
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
    void mainWindow_initialState_loadsGreetingAndControls() throws Exception {
        FxTestSupport.run(() -> {
            MainWindowFixture fixture = createWindow("initial-bola.txt");
            assertEquals(400, fixture.root().getPrefWidth());
            assertEquals(600, fixture.root().getPrefHeight());
            assertTrue(fixture.scroll().isFitToWidth());
            assertEquals(ScrollPane.ScrollBarPolicy.NEVER, fixture.scroll().getHbarPolicy());
            assertEquals(1, fixture.dialogs().getChildren().size());
            assertEquals("Bola: Eh hello! I'm Bola.\nGot anything to settle today?",
                    messageAt(fixture.dialogs(), 0));
            ImageView bolaPicture = (ImageView) ((DialogBox) fixture.dialogs()
                    .getChildren().getFirst())
                    .getChildren().getFirst();
            assertFalse(bolaPicture.getImage().isError());
            assertTrue(bolaPicture.getImage().getUrl().endsWith("DaBola.png"));
            return null;
        });
    }

    @Test
    void mainWindow_inputHandlers_addAndListTask() throws Exception {
        FxTestSupport.run(() -> {
            MainWindowFixture fixture = createWindow("input-bola.txt");
            TextField input = fixture.input();

            input.setText("   ");
            input.fireEvent(new ActionEvent());
            assertEquals(1, fixture.dialogs().getChildren().size());

            input.setText("todo read book");
            input.fireEvent(new ActionEvent());
            assertEquals(3, fixture.dialogs().getChildren().size());
            assertEquals("todo read book", messageAt(fixture.dialogs(), 1));
            assertTrue(messageAt(fixture.dialogs(), 2).contains("[T][ ] read book"));
            assertEquals("", input.getText());
            assertTrue(Files.readString(fixture.storage()).contains("read book"));
            ImageView userPicture = (ImageView) ((DialogBox) fixture.dialogs().getChildren().get(1))
                    .getChildren().getLast();
            assertFalse(userPicture.getImage().isError());
            assertTrue(userPicture.getImage().getUrl().endsWith("DaUser.png"));

            input.setText("list");
            fixture.send().fire();
            assertEquals(5, fixture.dialogs().getChildren().size());
            assertTrue(messageAt(fixture.dialogs(), 4).contains("1. [T][ ] read book"));
            assertEquals("", input.getText());
            fixture.root().applyCss();
            fixture.root().layout();
            assertEquals(1.0, fixture.scroll().getVvalue());
            return null;
        });
    }

    @Test
    void mainWindow_bye_disablesFurtherInput() throws Exception {
        FxTestSupport.run(() -> {
            MainWindowFixture fixture = createWindow("exit-bola.txt");
            fixture.input().setText("bye");
            fixture.send().fire();
            try {
                assertEquals(3, fixture.dialogs().getChildren().size());
                assertTrue(messageAt(fixture.dialogs(), 2).contains("All settled?"));
                assertTrue(fixture.input().isDisabled());
                assertTrue(fixture.send().isDisabled());
                fixture.input().setText("list");
                fixture.input().fireEvent(new ActionEvent());
                assertEquals(3, fixture.dialogs().getChildren().size());
            } finally {
                fixture.controller().stop();
            }
            return null;
        });
    }

    @Test
    void mainWindow_massDeleteConfirmation_usesSharedCommandState() throws Exception {
        FxTestSupport.run(() -> {
            MainWindowFixture fixture = createWindow("mass-bola.txt");
            for (String command : List.of("todo first", "todo second", "delete 1 2")) {
                fixture.input().setText(command);
                fixture.input().fireEvent(new ActionEvent());
            }
            assertEquals("Bola: U sure u want to delete these 2 tasks? (Yes/No)",
                    messageAt(fixture.dialogs(), 6));

            fixture.input().setText("yes");
            fixture.input().fireEvent(new ActionEvent());
            assertTrue(messageAt(fixture.dialogs(), 8).contains("No more tasks in your list"));
            assertEquals("", fixture.input().getText());
            fixture.controller().stop();
            return null;
        });
    }

    /**
     * Loads and lays out an isolated main window for one test.
     */
    private MainWindowFixture createWindow(String storageFileName) throws IOException {
        FXMLLoader loader = new FXMLLoader(MainWindow.class.getResource("/view/MainWindow.fxml"));
        AnchorPane root = loader.load();
        MainWindow controller = loader.getController();
        Path storage = temporaryDirectory.resolve(storageFileName);
        controller.setBola(new Bola(storage.toString()));
        new Scene(root);
        root.applyCss();
        root.layout();
        return new MainWindowFixture(root, controller, storage,
                (TextField) loader.getNamespace().get("userInput"),
                (Button) loader.getNamespace().get("sendButton"),
                (VBox) loader.getNamespace().get("dialogContainer"),
                (ScrollPane) loader.getNamespace().get("scrollPane"));
    }

    private String messageAt(VBox dialogs, int index) {
        DialogBox dialog = (DialogBox) dialogs.getChildren().get(index);
        return dialog.getChildren().stream().filter(Label.class::isInstance)
                .map(Label.class::cast).findFirst().orElseThrow().getText();
    }

    @Test
    void mainWindow_resize_keepsControlsAnchored() throws Exception {
        FxTestSupport.run(() -> {
            Platform.setImplicitExit(false);
            Main application = new Main();
            Stage stage = new Stage();
            try {
                application.start(stage);
                assertTrue(stage.isResizable());
                assertEquals(400, stage.getMinWidth());
                assertEquals(220, stage.getMinHeight());
                AnchorPane root = (AnchorPane) stage.getScene().getRoot();
                TextField input = (TextField) root.lookup("#userInput");
                Button send = (Button) root.lookup("#sendButton");
                ScrollPane scroll = (ScrollPane) root.lookup("#scrollPane");
                VBox dialogs = (VBox) root.lookup("#dialogContainer");

                for (double[] size : new double[][] {{800, 900}, {400, 220}, {600, 600}}) {
                    assertResponsiveLayout(root, input, send, scroll, dialogs, size);
                }
            } finally {
                application.stop();
                stage.hide();
            }
            return null;
        });
    }

    /**
     * Checks control anchoring and background behavior at one window size.
     */
    private void assertResponsiveLayout(AnchorPane root, TextField input, Button send,
            ScrollPane scroll, VBox dialogs, double[] size) {
        root.resize(size[0], size[1]);
        root.applyCss();
        root.layout();

        assertEquals(1, input.getLayoutX(), 0.01);
        assertEquals(size[0] - 66, input.getWidth(), 0.01);
        assertEquals(size[1] - 1, input.getLayoutY() + input.getHeight(), 0.01);
        assertEquals(size[0] - 1, send.getLayoutX() + send.getWidth(), 0.01);
        assertEquals(size[1] - 1, send.getLayoutY() + send.getHeight(), 0.01);
        assertTrue(input.getBoundsInParent().getMaxX() < send.getLayoutX());
        assertEquals(1, scroll.getLayoutX(), 0.01);
        assertEquals(1, scroll.getLayoutY(), 0.01);
        assertEquals(size[0] - 2, scroll.getWidth(), 0.01);
        assertEquals(size[1] - 43, scroll.getHeight(), 0.01);
        assertTrue(scroll.getBoundsInParent().getMaxY() <= input.getLayoutY());
        assertEquals(scroll.getViewportBounds().getWidth(), dialogs.getWidth(), 1);
        assertBackground(scroll, dialogs);
    }

    /**
     * Checks that the responsive background remains visible and transparent.
     */
    private void assertBackground(ScrollPane scroll, VBox dialogs) {
        BackgroundImage background = scroll.getBackground().getImages().getFirst();
        assertFalse(background.getImage().isError());
        assertTrue(background.getImage().getUrl().endsWith("bola-kopitiam-background.png"));
        assertTrue(background.getSize().isCover());
        assertEquals(BackgroundPosition.CENTER, background.getPosition());
        assertEquals(BackgroundRepeat.NO_REPEAT, background.getRepeatX());
        assertEquals(BackgroundRepeat.NO_REPEAT, background.getRepeatY());
        Region viewport = (Region) scroll.lookup(".viewport");
        assertFalse(viewport.getBackground().getFills().stream()
                .anyMatch(fill -> fill.getFill().isOpaque()));
        assertFalse(dialogs.getBackground().getFills().stream()
                .anyMatch(fill -> fill.getFill().isOpaque()));
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

    /**
     * Holds the controls loaded for one isolated main-window test.
     */
    private record MainWindowFixture(AnchorPane root, MainWindow controller, Path storage,
            TextField input, Button send, VBox dialogs, ScrollPane scroll) {
    }
}
