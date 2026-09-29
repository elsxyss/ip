package bola.ai;

/**
 * Answers natural-language questions about Bola's features.
 */
@FunctionalInterface
public interface AiService {
    /**
     * Returns an answer to a user's question about Bola.
     *
     * @param question question about Bola's features or commands.
     * @return AI-generated answer.
     * @throws AiException if the AI service cannot provide an answer.
     */
    String ask(String question) throws AiException;
}
