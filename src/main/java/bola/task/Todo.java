package bola.task;

/**
 * Represents a task that has no associated date or time.
 */
public class Todo extends Task {
    /**
     * Creates an incomplete to-do task.
     *
     * @param description description of the task.
     */
    public Todo(String description) {
        super(description);
    }

    /**
     * Returns the human-readable to-do type name.
     *
     * @return to-do type name.
     */
    @Override
    public String getTypeName() {
        return "To-do";
    }
}
