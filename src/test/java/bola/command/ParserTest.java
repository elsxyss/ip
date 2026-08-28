package bola.command;

import org.junit.jupiter.api.Test;

import bola.exception.BolaException;
import bola.task.Task;

/**
 * Checks command and argument parsing independently from the user interface.
 */
public class ParserTest {
    /**
     * Checks command recognition and numeric argument conversion.
     *
     * @throws BolaException if a valid input is unexpectedly rejected
     */
    @Test
    void testCommandAndNumberParsing() throws BolaException {
        Parser parser = new Parser();

        assert parser.parseCommandType("list") == CommandType.LIST;
        assert parser.parseCommandType("deadline submit /by 2026-09-01")
                == CommandType.DEADLINE;
        assert parser.parseTaskIndex("mark 2", CommandType.MARK, 3) == 1;
        assert parser.parseUpcomingDays("upcoming 7", CommandType.UPCOMING) == 7;
    }

    /**
     * Checks construction of each task type from command text.
     *
     * @throws BolaException if a valid input is unexpectedly rejected
     */
    @Test
    void testTaskParsing() throws BolaException {
        Parser parser = new Parser();

        Task todo = parser.parseTodo("todo read book");
        Task deadline = parser.parseDeadline("deadline submit report /by 2026-09-01");
        Task event = parser.parseEvent(
                "event demo /from 1/9/2026 1400 /to 1/9/2026 1500");

        assert todo.toDataString().equals("T | 0 | read book");
        assert deadline.toDataString().equals("D | 0 | submit report | 2026-09-01");
        assert event.toDataString().equals(
                "E | 0 | demo | 2026-09-01 1400 | 2026-09-01 1500");
    }

    /**
     * Checks representative validation failures and their user-facing explanations.
     *
     */
    @Test
    void testInvalidInput() {
        Parser parser = new Parser();

        assertParsingFails(() -> parser.parseCommandType("nonsense"), "Hmm?");
        assertParsingFails(() -> parser.parseTaskIndex("delete x", CommandType.DELETE, 2),
                "Please enter a valid task number to delete.");
        assertParsingFails(() -> parser.parseUpcomingDays("upcoming 0", CommandType.UPCOMING),
                "Please enter a positive number of days.");
        assertParsingFails(() -> parser.parseTodo("todo"),
                "What shall I add as your ToDo task?");
        assertParsingFails(() -> parser.parseDeadline("deadline submit report"),
                "When's your deadline for this task? (Please indicate with /by)");
        assertParsingFails(() -> parser.parseEvent("event demo /from today /to tomorrow"),
                "Please enter dates as yyyy-MM-dd, or include a time as "
                        + "d/M/yyyy HHmm (e.g., 2/12/2019 1800).");
    }

    /**
     * Checks that an operation fails with the expected explanation.
     *
     * @param operation parser operation expected to fail
     * @param expectedMessage expected user-facing explanation
     */
    private static void assertParsingFails(ParsingOperation operation, String expectedMessage) {
        try {
            operation.run();
            assert false : "Parsing should fail";
        } catch (BolaException exception) {
            assert exception.getMessage().equals(expectedMessage);
        }
    }

    /**
     * Represents a parser operation that can reject invalid user input.
     */
    @FunctionalInterface
    private interface ParsingOperation {
        /**
         * Runs the parser operation.
         *
         * @throws BolaException if the supplied input is invalid
         */
        void run() throws BolaException;
    }
}
