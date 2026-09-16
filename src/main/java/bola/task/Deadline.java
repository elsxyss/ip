package bola.task;

import java.time.LocalDateTime;

/**
 * Represents a task that must be completed by a particular date or time.
 */
public class Deadline extends Task {
    /** Date and time by which this task must be completed. */
    protected LocalDateTime byDate;

    /**
     * Creates an incomplete deadline task.
     *
     * @param description description of the task.
     * @param byDate date and optional time by which the task must be completed.
     */
    public Deadline(String description, LocalDateTime byDate) {
        super(description);
        this.byDate = byDate;
    }

    /**
     * Creates an incomplete deadline task from a supported date string.
     *
     * @param description description of the task.
     * @param byDate date in a format supported by {@link TaskDateTime#parse(String)}.
     */
    public Deadline(String description, String byDate) {
        this(description, TaskDateTime.parse(byDate));
    }

    /**
     * Returns when this task is due.
     *
     * @return deadline date and time.
     */
    public LocalDateTime getByDate() {
        return byDate;
    }

    /**
     * Returns whether another task is a deadline with the same description and due date.
     *
     * @param other task to compare with this deadline.
     * @return true if both deadlines represent the same work.
     */
    @Override
    public boolean hasSameDetails(Task other) {
        return super.hasSameDetails(other)
                && byDate.equals(((Deadline) other).byDate);
    }

    /**
     * Returns this deadline in the format used for saving it to the hard disk.
     *
     * @return serialized deadline data.
     */
    @Override
    public String toDataString() {
        return "D | " + getDataStatus() + " | " + escapeDataField(description)
                + " | " + TaskDateTime.formatForStorage(byDate);
    }

    /**
     * Returns the human-readable deadline type name.
     *
     * @return deadline type name.
     */
    @Override
    public String getTypeName() {
        return "Deadline";
    }

    /**
     * Returns the description followed by the deadline.
     *
     * @return user-facing deadline details.
     */
    @Override
    public String getDisplayDetails() {
        return description + " (By: " + TaskDateTime.formatForDisplay(byDate) + ")";
    }
}
