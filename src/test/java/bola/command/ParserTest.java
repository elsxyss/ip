package bola.command;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import bola.exception.BolaException;
import bola.task.Deadline;
import bola.task.Event;
import bola.task.Task;

/**
 * Tests command and argument parsing independently from the user interface.
 */
public class ParserTest {
    private Parser parser;

    /**
     * Creates a fresh parser for each test.
     */
    @BeforeEach
    void setUp() {
        parser = new Parser();
    }

    /**
     * Checks every supported command and the boundary between commands and ordinary words.
     */
    @Test
    void parseCommandType_validAndSimilarInputs_returnsCommandOrThrows() throws BolaException {
        assertAll(
                () -> assertEquals(CommandType.BYE, parser.parseCommandType("bye")),
                () -> assertEquals(CommandType.LIST, parser.parseCommandType("list")),
                () -> assertEquals(CommandType.UPCOMING,
                        parser.parseCommandType("upcoming 7")),
                () -> assertEquals(CommandType.MARK, parser.parseCommandType("mark 1")),
                () -> assertEquals(CommandType.UNMARK, parser.parseCommandType("unmark 1")),
                () -> assertEquals(CommandType.DELETE, parser.parseCommandType("delete 1")),
                () -> assertEquals(CommandType.TODO, parser.parseCommandType("todo read")),
                () -> assertEquals(CommandType.DEADLINE,
                        parser.parseCommandType("deadline submit /by 2026-09-01")),
                () -> assertEquals(CommandType.EVENT,
                        parser.parseCommandType("event lesson /from 2026-09-01 /to 2026-09-02")),
                () -> assertParsingFails(() -> parser.parseCommandType("listing"), "Hmm?"),
                () -> assertParsingFails(() -> parser.parseCommandType("list now"), "Hmm?"),
                () -> assertParsingFails(() -> parser.parseCommandType("TODO read"), "Hmm?"),
                () -> assertParsingFails(() -> parser.parseCommandType(""), "Hmm?"));
    }

    /**
     * Checks conversion from one-based task numbers to zero-based list indexes.
     */
    @Test
    void parseTaskIndex_boundaryAndInvalidNumbers_returnsIndexOrThrows() throws BolaException {
        assertAll(
                () -> assertEquals(0, parser.parseTaskIndex("mark 1", CommandType.MARK, 3)),
                () -> assertEquals(2, parser.parseTaskIndex("mark 3", CommandType.MARK, 3)),
                () -> assertEquals(1,
                        parser.parseTaskIndex("delete   2  ", CommandType.DELETE, 3)),
                () -> assertParsingFails(() -> parser.parseTaskIndex(
                        "mark", CommandType.MARK, 3), "Which task number shall I mark?"),
                () -> assertParsingFails(() -> parser.parseTaskIndex(
                        "mark 0", CommandType.MARK, 3), "There is no task numbered 0."),
                () -> assertParsingFails(() -> parser.parseTaskIndex(
                        "mark 4", CommandType.MARK, 3), "There is no task numbered 4."),
                () -> assertParsingFails(() -> parser.parseTaskIndex(
                        "mark -1", CommandType.MARK, 3), "There is no task numbered -1."),
                () -> assertParsingFails(() -> parser.parseTaskIndex(
                        "delete two", CommandType.DELETE, 3),
                        "Please enter a valid task number to delete."),
                () -> assertParsingFails(() -> parser.parseTaskIndex(
                        "delete 1 2", CommandType.DELETE, 3),
                        "Please enter a valid task number to delete."));
    }

    /**
     * Checks that upcoming accepts positive whole numbers only.
     */
    @Test
    void parseUpcomingDays_boundaryAndInvalidValues_returnsDaysOrThrows() throws BolaException {
        assertAll(
                () -> assertEquals(1,
                        parser.parseUpcomingDays("upcoming 1", CommandType.UPCOMING)),
                () -> assertEquals(365,
                        parser.parseUpcomingDays("upcoming   365  ", CommandType.UPCOMING)),
                () -> assertParsingFails(() -> parser.parseUpcomingDays(
                        "upcoming", CommandType.UPCOMING),
                        "How many days ahead shall I check? (e.g., upcoming 7)"),
                () -> assertParsingFails(() -> parser.parseUpcomingDays(
                        "upcoming 0", CommandType.UPCOMING),
                        "Please enter a positive number of days."),
                () -> assertParsingFails(() -> parser.parseUpcomingDays(
                        "upcoming -2", CommandType.UPCOMING),
                        "Please enter a positive number of days."),
                () -> assertParsingFails(() -> parser.parseUpcomingDays(
                        "upcoming 1.5", CommandType.UPCOMING),
                        "Please enter a whole number of days (e.g., upcoming 7)."));
    }

    /**
     * Checks to-do parsing and required-description validation.
     */
    @Test
    void parseTodo_presentOrMissingDescription_returnsTaskOrThrows() throws BolaException {
        Task todo = parser.parseTodo("todo   read book  ");

        assertEquals("T | 0 | read book", todo.toDataString());
        assertAll(
                () -> assertParsingFails(() -> parser.parseTodo("todo"),
                        "What shall I add as your ToDo task?"),
                () -> assertParsingFails(() -> parser.parseTodo("todo   "),
                        "What shall I add as your ToDo task?"));
    }

    /**
     * Checks both accepted deadline date formats and each required component.
     */
    @Test
    void parseDeadline_validAndIncompleteInputs_returnsTaskOrThrows() throws BolaException {
        Deadline dateOnly = (Deadline) parser.parseDeadline(
                "deadline submit report /by 2026-09-01");
        Deadline dateAndTime = (Deadline) parser.parseDeadline(
                "deadline demo /by 1/9/2026 1400");

        assertAll(
                () -> assertEquals("D | 0 | submit report | 2026-09-01",
                        dateOnly.toDataString()),
                () -> assertEquals("D | 0 | demo | 2026-09-01 1400",
                        dateAndTime.toDataString()),
                () -> assertParsingFails(() -> parser.parseDeadline("deadline"),
                        "What shall I add as your Deadline task?"),
                () -> assertParsingFails(() -> parser.parseDeadline("deadline /by 2026-09-01"),
                        "What shall I add as your Deadline task?"),
                () -> assertParsingFails(() -> parser.parseDeadline("deadline submit report"),
                        "When's your deadline for this task? (Please indicate with /by)"),
                () -> assertParsingFails(() -> parser.parseDeadline("deadline submit /by"),
                        "When's your deadline for this task? (Please indicate with /by)"),
                () -> assertParsingFails(() -> parser.parseDeadline(
                        "deadline submit /by 2026-02-29"), invalidDateMessage()));
    }

    /**
     * Checks event construction, separator validation, and invalid dates.
     */
    @Test
    void parseEvent_validAndIncompleteInputs_returnsTaskOrThrows() throws BolaException {
        Event event = (Event) parser.parseEvent(
                "event demo /from 1/9/2026 1400 /to 1/9/2026 1500");

        assertEquals("E | 0 | demo | 2026-09-01 1400 | 2026-09-01 1500",
                event.toDataString());
        assertAll(
                () -> assertParsingFails(() -> parser.parseEvent("event"),
                        "What shall I add as your Event task?"),
                () -> assertParsingFails(() -> parser.parseEvent(
                        "event /from 2026-09-01 /to 2026-09-02"),
                        "What shall I add as your Event task?"),
                () -> assertParsingFails(() -> parser.parseEvent("event demo"),
                        "When's this event happening? (Please indicate with /from and /to)"),
                () -> assertParsingFails(() -> parser.parseEvent(
                        "event demo /from 2026-09-01"),
                        "When's this event happening? (Please indicate with /from and /to)"),
                () -> assertParsingFails(() -> parser.parseEvent(
                        "event demo /from /to 2026-09-02"),
                        "When's this event happening? (Please indicate with /from and /to)"),
                () -> assertParsingFails(() -> parser.parseEvent(
                        "event demo /from today /to 2026-09-02"), invalidDateMessage()));
    }

    /**
     * Checks that an operation fails with the expected user-facing explanation.
     *
     * @param operation parser operation expected to fail.
     * @param expectedMessage expected user-facing explanation.
     */
    private static void assertParsingFails(ParsingOperation operation, String expectedMessage) {
        BolaException exception = assertThrows(BolaException.class, operation::run);
        assertEquals(expectedMessage, exception.getMessage());
    }

    /**
     * Returns the shared explanation for an unsupported or impossible date.
     *
     * @return invalid-date message.
     */
    private static String invalidDateMessage() {
        return "Please enter dates as yyyy-MM-dd, or include a time as "
                + "d/M/yyyy HHmm (e.g., 2/12/2019 1800).";
    }

    /**
     * Represents a parser operation that can reject invalid user input.
     */
    @FunctionalInterface
    private interface ParsingOperation {
        /**
         * Runs the parser operation.
         *
         * @throws BolaException if the supplied input is invalid.
         */
        void run() throws BolaException;
    }
}
