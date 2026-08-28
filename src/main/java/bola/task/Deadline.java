package bola.task;

import java.time.LocalDateTime;

/**
 * Represents a task that must be completed by a particular date or time.
 */
public class Deadline extends Task {
    protected LocalDateTime by;

    /**
     * Creates an incomplete deadline task.
     *
     * @param description description of the task
     * @param by date and optional time by which the task must be completed
     */
    public Deadline(String description, LocalDateTime by) {
        super(description);
        this.by = by;
    }

    /**
     * Creates an incomplete deadline task from a supported date string.
     *
     * @param description description of the task
     * @param by date in a format supported by {@link TaskDateTime#parse(String)}
     */
    public Deadline(String description, String by) {
        this(description, TaskDateTime.parse(by));
    }

    /**
     * Returns when this task is due.
     *
     * @return deadline date and time
     */
    public LocalDateTime getBy() {
        return by;
    }

    /**
     * Returns this deadline in the format used for saving it to the hard disk.
     *
     * @return serialized deadline data
     */
    @Override
    public String toDataString() {
        return "D | " + getDataStatus() + " | " + escapeDataField(description)
                + " | " + TaskDateTime.formatForStorage(by);
    }

    /**
     * Returns the deadline in Bola's display format.
     *
     * @return the task prefixed with its type marker and followed by its deadline
     */
    @Override
    public String toString() {
        return "[D]" + super.toString() + " (By: " + TaskDateTime.formatForDisplay(by) + ")";
    }
}
