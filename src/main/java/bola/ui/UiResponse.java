package bola.ui;

import java.util.List;

/**
 * Contains response text and its presentation type for the graphical interface.
 */
public record UiResponse(String text, ResponseType type, List<TaskView> taskViews) {
    /**
     * Creates a response without interactive task rows.
     */
    public UiResponse(String text, ResponseType type) {
        this(text, type, List.of());
    }

    /**
     * Creates an immutable snapshot of a GUI response.
     */
    public UiResponse {
        taskViews = List.copyOf(taskViews);
    }
}
