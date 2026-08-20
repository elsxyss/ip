/**
 * Represents a task that takes place between a start and an end date or time.
 */
public class Event extends Task {
    protected String from;
    protected String to;

    /**
     * Creates an incomplete event task.
     *
     * @param description description of the task
     * @param from date or time at which the event starts
     * @param to date or time at which the event ends
     */
    public Event(String description, String from, String to) {
        super(description);
        this.from = from;
        this.to = to;
    }

    /**
     * Returns the event in Bola's display format.
     *
     * @return the task prefixed with its type marker and followed by its time range
     */
    @Override
    public String toString() {
        return "[E]" + super.toString() + " (" + from + " – " + to + ")";
    }
}
