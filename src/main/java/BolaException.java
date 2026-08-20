/**
 * Represents an error caused by a command that Bola cannot carry out.
 */
public class BolaException extends Exception {
    /** Identifies the serialized form of this exception class. */
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with an explanation that can be shown to the user.
     *
     * @param message explanation of the invalid command and how to correct it
     */
    public BolaException(String message) {
        super(message);
    }
}
