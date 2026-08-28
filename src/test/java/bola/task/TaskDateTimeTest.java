package bola.task;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.Test;

/**
 * Checks parsing, display, and storage formats for task dates and times.
 */
public class TaskDateTimeTest {
    /**
     * Checks valid and invalid date-time parsing and formatting.
     */
    @Test
    void testParsingAndFormatting() {
        LocalDateTime dateOnly = TaskDateTime.parse("2019-10-15");
        assert dateOnly.equals(LocalDateTime.of(2019, 10, 15, 0, 0));
        assert TaskDateTime.formatForDisplay(dateOnly).equals("Oct 15 2019");
        assert TaskDateTime.formatForStorage(dateOnly).equals("2019-10-15");

        LocalDateTime dateAndTime = TaskDateTime.parse("2/12/2019 1800");
        assert dateAndTime.equals(LocalDateTime.of(2019, 12, 2, 18, 0));
        assert TaskDateTime.formatForDisplay(dateAndTime).equals("Dec 02 2019 6:00 PM");
        assert TaskDateTime.formatForStorage(dateAndTime).equals("2019-12-02 1800");

        assertParseFails("2019-02-29");
        assertParseFails("Oct 15 2019");
    }

    /**
     * Checks that an invalid date cannot be parsed.
     *
     * @param value invalid date value
     */
    private static void assertParseFails(String value) {
        try {
            TaskDateTime.parse(value);
            assert false : "Parsing should fail: " + value;
        } catch (DateTimeParseException exception) {
            assert true;
        }
    }
}
