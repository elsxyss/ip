package bola.task;

/**
 * Represents a task and whether it has been completed.
 */
public class Task {
    /** Human-readable description of this task. */
    protected String description;
    /** Whether this task has been completed. */
    protected boolean isDone;

    /**
     * Creates an incomplete task with the given description.
     *
     * @param description description of the task.
     */
    public Task(String description) {
        this.description = description;
        this.isDone = false;
    }

    /**
     * Returns the symbol used to display the task's completion status.
     *
     * @return {@code X} if the task is done, or a space otherwise.
     */
    public String getStatusIcon() {
        return isDone ? "X" : " ";
    }

    /**
     * Returns the task description.
     *
     * @return task description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Marks this task as completed.
     */
    public void markAsDone() {
        isDone = true;
    }

    /**
     * Marks this task as not completed.
     */
    public void markAsNotDone() {
        isDone = false;
    }

    /**
     * Returns whether this task has been completed as a value suitable for the data file.
     *
     * @return {@code 1} if done, or {@code 0} otherwise.
     */
    protected String getDataStatus() {
        return isDone ? "1" : "0";
    }

    /**
     * Returns this task in the format used for saving it to the hard disk.
     *
     * @return serialized task data.
     */
    public String toDataString() {
        return "T | " + getDataStatus() + " | " + escapeDataField(description);
    }

    /**
     * Escapes characters that otherwise have a special meaning in Bola's data format.
     *
     * @param value task field to escape.
     * @return field with backslashes and vertical bars escaped.
     */
    protected String escapeDataField(String value) {
        return value.replace("\\", "\\\\").replace("|", "\\|");
    }

    /**
     * Returns the task in the format used by Bola's responses.
     *
     * @return the status icon followed by the task description.
     */
    @Override
    public String toString() {
        return "[" + getStatusIcon() + "] " + description;
    }
}
