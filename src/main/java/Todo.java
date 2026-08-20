/**
 * Represents a task that has no associated date or time.
 */
public class Todo extends Task {
    /**
     * Creates an incomplete to-do task.
     *
     * @param description description of the task
     */
    public Todo(String description) {
        super(description);
    }

    /**
     * Returns the to-do task in Bola's display format.
     *
     * @return the task prefixed with its type marker
     */
    @Override
    public String toString() {
        return "[T]" + super.toString();
    }
}
