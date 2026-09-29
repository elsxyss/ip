package bola.ai;

/**
 * Signals that the remote AI service could not provide a usable response.
 */
public class AiException extends Exception {
    /**
     * Creates an AI service exception caused by another failure.
     *
     * @param cause underlying service failure.
     */
    public AiException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates an AI service exception with an explanation.
     *
     * @param message explanation of the failure.
     */
    public AiException(String message) {
        super(message);
    }
}
