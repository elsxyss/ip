package bola.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Controls an FXML chat bubble and its speaker's avatar.
 */
public class DialogBox extends HBox {
    /** Keeps user commands compact even when the window is wide. */
    private static final double MAXIMUM_USER_BUBBLE_WIDTH = 270;
    /** Gives Bola's longer replies and task lists more horizontal room. */
    private static final double MAXIMUM_BOLA_BUBBLE_WIDTH = 360;
    /** Fits beside the fixed-width task type badge inside a Bola bubble. */
    private static final double MAXIMUM_TASK_CONTROL_WIDTH = 267;
    /** Ignores nearly invisible export artifacts when locating the avatar artwork. */
    private static final int MIN_VISIBLE_ALPHA = 8;
    /** Number of bits before the alpha component in an ARGB pixel value. */
    private static final int ALPHA_BIT_SHIFT = 24;

    @FXML
    private Label text;
    @FXML
    private ImageView displayPicture;

    private final List<CheckBox> taskControls = new ArrayList<>();

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
                int alpha = image.getPixelReader().getArgb(x, y) >>> ALPHA_BIT_SHIFT;
                if (alpha > MIN_VISIBLE_ALPHA) {
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
        DialogBox dialog = new DialogBox(message, image);
        dialog.getStyleClass().add("user-dialog");
        dialog.text.setMaxWidth(MAXIMUM_USER_BUBBLE_WIDTH);
        return dialog;
    }

    /**
     * Returns a Bola message with the avatar on the left.
     */
    public static DialogBox getBolaDialog(String message, Image image) {
        return getBolaDialog(new UiResponse(message, ResponseType.NORMAL), image);
    }

    /**
     * Returns a typed Bola message styled for normal, error, or warning content.
     */
    public static DialogBox getBolaDialog(UiResponse response, Image image) {
        return getBolaDialog(response, image, null);
    }

    /**
     * Returns a typed Bola message whose task rows invoke the supplied completion handler.
     */
    public static DialogBox getBolaDialog(UiResponse response, Image image,
            BiFunction<Integer, Boolean, Boolean> taskToggleHandler) {
        DialogBox dialog = new DialogBox(response.text(), image);
        dialog.flip();
        dialog.text.setMaxWidth(MAXIMUM_BOLA_BUBBLE_WIDTH);
        dialog.addTaskControls(response, taskToggleHandler);
        switch (response.type()) {
            case ERROR -> dialog.getStyleClass().add("error-dialog");
            case WARNING -> dialog.getStyleClass().add("warning-dialog");
            case NORMAL -> {
                // The standard Bola style is already applied by flip().
            }
            default -> throw new AssertionError("Every response type must be styled explicitly");
        }
        return dialog;
    }

    /**
     * Disables task controls whose displayed numbers may no longer match the current task list.
     */
    void disableTaskControls() {
        taskControls.forEach(control -> control.setDisable(true));
    }

    /**
     * Adds one accessible checkbox and type badge for each task in a list response.
     */
    private void addTaskControls(UiResponse response,
            BiFunction<Integer, Boolean, Boolean> taskToggleHandler) {
        if (response.taskViews().isEmpty()) {
            return;
        }

        text.setText(response.text().lines().findFirst().orElse(""));
        text.setContentDisplay(ContentDisplay.BOTTOM);
        text.setGraphicTextGap(8);
        VBox taskList = new VBox(7);
        taskList.getStyleClass().add("task-list");
        for (TaskView taskView : response.taskViews()) {
            taskList.getChildren().add(createTaskRow(taskView, taskToggleHandler));
        }
        text.setGraphic(taskList);
    }

    /**
     * Creates one task row and safely restores its state if the operation is rejected.
     */
    private HBox createTaskRow(TaskView taskView,
            BiFunction<Integer, Boolean, Boolean> taskToggleHandler) {
        Label typeBadge = new Label(taskView.typeName());
        typeBadge.getStyleClass().add("task-type");

        CheckBox completion = new CheckBox(taskView.number() + ". " + taskView.details());
        completion.getStyleClass().add("task-checkbox");
        completion.setSelected(taskView.isDone());
        completion.setWrapText(true);
        completion.setMaxWidth(MAXIMUM_TASK_CONTROL_WIDTH);
        HBox.setHgrow(completion, Priority.ALWAYS);
        taskControls.add(completion);
        if (taskToggleHandler == null) {
            completion.setDisable(true);
        } else {
            completion.setOnAction(event -> {
                boolean requestedState = completion.isSelected();
                boolean isAccepted = taskToggleHandler.apply(taskView.number(), requestedState);
                if (!isAccepted) {
                    completion.setSelected(!requestedState);
                }
            });
        }

        HBox row = new HBox(7, typeBadge, completion);
        row.setAlignment(Pos.TOP_LEFT);
        row.getStyleClass().add("task-row");
        return row;
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
