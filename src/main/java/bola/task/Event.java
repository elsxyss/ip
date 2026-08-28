package bola.task;

import java.time.LocalDateTime;

/**
 * Represents a task that takes place between a start and an end date or time.
 */
public class Event extends Task {
    /** Date and time at which this event starts. */
    protected LocalDateTime from;
    /** Date and time at which this event ends. */
    protected LocalDateTime to;

    /**
     * Creates an incomplete event task.
     *
     * @param description description of the task.
     * @param from date and optional time at which the event starts.
     * @param to date and optional time at which the event ends.
     */
    public Event(String description, LocalDateTime from, LocalDateTime to) {
        super(description);
        this.from = from;
        this.to = to;
    }

    /**
     * Creates an incomplete event task from supported date strings.
     *
     * @param description description of the task.
     * @param from date in a format supported by {@link TaskDateTime#parse(String)}.
     * @param to date in a format supported by {@link TaskDateTime#parse(String)}.
     */
    public Event(String description, String from, String to) {
        this(description, TaskDateTime.parse(from), TaskDateTime.parse(to));
    }

    /**
     * Returns when this event starts.
     *
     * @return event start date and time.
     */
    public LocalDateTime getFrom() {
        return from;
    }

    /**
     * Returns when this event ends.
     *
     * @return event end date and time.
     */
    public LocalDateTime getTo() {
        return to;
    }

    /**
     * Returns this event in the format used for saving it to the hard disk.
     *
     * @return serialized event data.
     */
    @Override
    public String toDataString() {
        return "E | " + getDataStatus() + " | " + escapeDataField(description)
                + " | " + TaskDateTime.formatForStorage(from)
                + " | " + TaskDateTime.formatForStorage(to);
    }

    /**
     * Returns the event in Bola's display format.
     *
     * @return the task prefixed with its type marker and followed by its time range.
     */
    @Override
    public String toString() {
        return "[E]" + super.toString() + " ("
                + TaskDateTime.formatForDisplay(from) + " – "
                + TaskDateTime.formatForDisplay(to) + ")";
    }
}
