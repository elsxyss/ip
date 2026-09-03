package bola.task;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.Test;

/**
 * Tests parsing, display, and storage formats for task dates and times.
 */
public class TaskDateTimeTest {
    /**
     * Checks that dated tasks expose their clearly named date values.
     */
    @Test
    void datedTaskGetters_constructedDates_returnExpectedValues() {
        LocalDateTime deadlineDate = LocalDateTime.of(2026, 9, 10, 18, 0);
        LocalDateTime eventStartDate = LocalDateTime.of(2026, 9, 11, 14, 0);
        LocalDateTime eventEndDate = LocalDateTime.of(2026, 9, 11, 16, 0);
        Deadline deadline = new Deadline("submit report", deadlineDate);
        Event event = new Event("project meeting", eventStartDate, eventEndDate);

        assertAll(
                () -> assertEquals(deadlineDate, deadline.getByDate()),
                () -> assertEquals(eventStartDate, event.getStartDate()),
                () -> assertEquals(eventEndDate, event.getEndDate()));
    }

    /**
     * Checks every supported input format, including leap-day and single-digit values.
     */
    @Test
    void parse_supportedFormats_returnsDateTime() {
        assertAll(
                () -> assertEquals(LocalDateTime.of(2019, 10, 15, 0, 0),
                        TaskDateTime.parse("2019-10-15")),
                () -> assertEquals(LocalDateTime.of(2019, 12, 2, 18, 0),
                        TaskDateTime.parse("2/12/2019 1800")),
                () -> assertEquals(LocalDateTime.of(2019, 12, 2, 8, 5),
                        TaskDateTime.parse("2019-12-02 0805")),
                () -> assertEquals(LocalDateTime.of(2024, 2, 29, 0, 0),
                        TaskDateTime.parse("2024-02-29")));
    }

    /**
     * Checks impossible dates, times, and unsupported formats are rejected strictly.
     */
    @Test
    void parse_invalidValues_throwsDateTimeParseException() {
        assertAll(
                () -> assertThrows(DateTimeParseException.class,
                        () -> TaskDateTime.parse("2019-02-29")),
                () -> assertThrows(DateTimeParseException.class,
                        () -> TaskDateTime.parse("2019-13-01")),
                () -> assertThrows(DateTimeParseException.class,
                        () -> TaskDateTime.parse("2019-12-02 2400")),
                () -> assertThrows(DateTimeParseException.class,
                        () -> TaskDateTime.parse("2019-12-02 8:05")),
                () -> assertThrows(DateTimeParseException.class,
                        () -> TaskDateTime.parse("Oct 15 2019")),
                () -> assertThrows(DateTimeParseException.class,
                        () -> TaskDateTime.parse("")));
    }

    /**
     * Checks display formatting chooses date-only and date-time forms correctly.
     */
    @Test
    void formatForDisplay_midnightAndNonMidnight_formatsForUser() {
        assertAll(
                () -> assertEquals("Dec 02 2019", TaskDateTime.formatForDisplay(
                        LocalDateTime.of(2019, 12, 2, 0, 0))),
                () -> assertEquals("Dec 02 2019 6:00 PM", TaskDateTime.formatForDisplay(
                        LocalDateTime.of(2019, 12, 2, 18, 0))),
                () -> assertEquals("Dec 02 2019 8:05 AM", TaskDateTime.formatForDisplay(
                        LocalDateTime.of(2019, 12, 2, 8, 5))));
    }

    /**
     * Checks storage formatting is stable and can be parsed back without losing information.
     */
    @Test
    void formatForStorage_dateTimes_roundTripsThroughParser() {
        LocalDateTime dateOnly = LocalDateTime.of(2019, 12, 2, 0, 0);
        LocalDateTime dateAndTime = LocalDateTime.of(2019, 12, 2, 8, 5);

        String storedDate = TaskDateTime.formatForStorage(dateOnly);
        String storedDateTime = TaskDateTime.formatForStorage(dateAndTime);

        assertAll(
                () -> assertEquals("2019-12-02", storedDate),
                () -> assertEquals("2019-12-02 0805", storedDateTime),
                () -> assertEquals(dateOnly, TaskDateTime.parse(storedDate)),
                () -> assertEquals(dateAndTime, TaskDateTime.parse(storedDateTime)));
    }
}
