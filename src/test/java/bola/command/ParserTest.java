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
                () -> assertEquals(CommandType.FIND, parser.parseCommandType("find book")),
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
                () -> assertParsingFails(() -> parser.parseCommandType("listing"),
                        "I don't understand that command leh."),
                () -> assertParsingFails(() -> parser.parseCommandType("list now"),
                        "I don't understand that command leh."),
                () -> assertParsingFails(() -> parser.parseCommandType("finder"),
                        "I don't understand that command leh."),
                () -> assertParsingFails(() -> parser.parseCommandType("TODO read"),
                        "I don't understand that command leh."),
                () -> assertParsingFails(() -> parser.parseCommandType(""),
                        "I don't understand that command leh."));
    }

    /**
     * Checks that find requires a keyword and preserves internal spaces in search text.
     */
    @Test
    void parseFindKeyword_presentOrMissingKeyword_returnsKeywordOrThrows() throws BolaException {
        assertAll(
                () -> assertEquals("read   book", parser.parseFindKeyword(
                        "find   read   book  ")),
                () -> assertParsingFails(() -> parser.parseFindKeyword("find"),
                        "what keyword should I search for? Give me one, can?"),
                () -> assertParsingFails(() -> parser.parseFindKeyword("find   "),
                        "what keyword should I search for? Give me one, can?"));
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
                        "mark", CommandType.MARK, 3),
                        "which task number you want me to mark?"),
                () -> assertParsingFails(() -> parser.parseTaskIndex(
                        "mark 0", CommandType.MARK, 3),
                        "task number 0 doesn't exist leh."),
                () -> assertParsingFails(() -> parser.parseTaskIndex(
                        "mark 4", CommandType.MARK, 3),
                        "task number 4 doesn't exist leh."),
                () -> assertParsingFails(() -> parser.parseTaskIndex(
                        "mark -1", CommandType.MARK, 3),
                        "task number -1 doesn't exist leh."),
                () -> assertParsingFails(() -> parser.parseTaskIndex(
                        "delete two", CommandType.DELETE, 3),
                        "please give me a valid task number to delete, can?"),
                () -> assertParsingFails(() -> parser.parseTaskIndex(
                        "delete 1 2", CommandType.DELETE, 3),
                        "please give me a valid task number to delete, can?"));
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
                        "how many days ahead should I check? Try upcoming 7."),
                () -> assertParsingFails(() -> parser.parseUpcomingDays(
                        "upcoming 0", CommandType.UPCOMING),
                        "the number of days must be positive, can?"),
                () -> assertParsingFails(() -> parser.parseUpcomingDays(
                        "upcoming -2", CommandType.UPCOMING),
                        "the number of days must be positive, can?"),
                () -> assertParsingFails(() -> parser.parseUpcomingDays(
                        "upcoming 1.5", CommandType.UPCOMING),
                        "please use a whole number of days—for example, upcoming 7."));
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
                        "what to-do should I add for you?"),
                () -> assertParsingFails(() -> parser.parseTodo("todo   "),
                        "what to-do should I add for you?"));
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
                        "what deadline task should I add for you?"),
                () -> assertParsingFails(() -> parser.parseDeadline("deadline /by 2026-09-01"),
                        "what deadline task should I add for you?"),
                () -> assertParsingFails(() -> parser.parseDeadline("deadline submit report"),
                        "when is this due? Use /by to tell me, can?"),
                () -> assertParsingFails(() -> parser.parseDeadline("deadline submit /by"),
                        "when is this due? Add a date after /by, can?"),
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
                        "what event should I add for you?"),
                () -> assertParsingFails(() -> parser.parseEvent(
                        "event /from 2026-09-01 /to 2026-09-02"),
                        "what event should I add for you?"),
                () -> assertParsingFails(() -> parser.parseEvent("event demo"),
                        "when is this event happening? Use /from and /to, can?"),
                () -> assertParsingFails(() -> parser.parseEvent(
                        "event demo /from 2026-09-01"),
                        "when is this event happening? Use /from and /to, can?"),
                () -> assertParsingFails(() -> parser.parseEvent(
                        "event demo /from /to 2026-09-02"),
                        "I need both the start and end times. Use /from and /to, can?"),
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
        return "this date cannot leh. Use yyyy-MM-dd, or d/M/yyyy HHmm "
                + "when including a time—for example, 2/12/2019 1800.";
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
