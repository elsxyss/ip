package bola.task;

import java.time.LocalDateTime;

/**
 * Represents a task that takes place between a start and an end date or time.
 */
public class Event extends Task {
    /** Date and time at which this event starts. */
    protected LocalDateTime startDate;
    /** Date and time at which this event ends. */
    protected LocalDateTime endDate;

    /**
     * Creates an incomplete event task.
     *
     * @param description description of the task.
     * @param startDate date and optional time at which the event starts.
     * @param endDate date and optional time at which the event ends.
     */
    public Event(String description, LocalDateTime startDate, LocalDateTime endDate) {
        super(description);
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /**
     * Creates an incomplete event task from supported date strings.
     *
     * @param description description of the task.
     * @param startDate date in a format supported by {@link TaskDateTime#parse(String)}.
     * @param endDate date in a format supported by {@link TaskDateTime#parse(String)}.
     */
    public Event(String description, String startDate, String endDate) {
        this(description, TaskDateTime.parse(startDate), TaskDateTime.parse(endDate));
    }

    /**
     * Returns when this event starts.
     *
     * @return event start date and time.
     */
    public LocalDateTime getStartDate() {
        return startDate;
    }

    /**
     * Returns when this event ends.
     *
     * @return event end date and time.
     */
    public LocalDateTime getEndDate() {
        return endDate;
    }

    /**
     * Returns this event in the format used for saving it to the hard disk.
     *
     * @return serialized event data.
     */
    @Override
    public String toDataString() {
        return "E | " + getDataStatus() + " | " + escapeDataField(description)
                + " | " + TaskDateTime.formatForStorage(startDate)
                + " | " + TaskDateTime.formatForStorage(endDate);
    }

    /**
     * Returns the event in Bola's display format.
     *
     * @return the task prefixed with its type marker and followed by its time range.
     */
    @Override
    public String toString() {
        return "[E]" + super.toString() + " ("
                + TaskDateTime.formatForDisplay(startDate) + " – "
                + TaskDateTime.formatForDisplay(endDate) + ")";
    }
}
