/**
 * Represents a task that must be completed by a particular date or time.
 */
public class Deadline extends Task {
    protected String by;

    /**
     * Creates an incomplete deadline task.
     *
     * @param description description of the task
     * @param by date or time by which the task must be completed
     */
    public Deadline(String description, String by) {
        super(description);
        this.by = by;
    }

    /**
     * Returns the deadline in Bola's display format.
     *
     * @return the task prefixed with its type marker and followed by its deadline
     */
    @Override
    public String toString() {
        return "[D]" + super.toString() + " (By: " + by + ")";
    }
}
