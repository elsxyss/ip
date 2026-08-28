package bola.task;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;

/**
 * Parses and formats the dates and times attached to tasks.
 */
public final class TaskDateTime {
    private static final DateTimeFormatter ISO_DATE = strictFormatter("uuuu-MM-dd");
    private static final List<DateTimeFormatter> DATE_TIME_INPUT_FORMATTERS = List.of(
            strictFormatter("uuuu-MM-dd HHmm"),
            strictFormatter("d/M/uuuu HHmm"));
    private static final DateTimeFormatter DISPLAY_DATE = formatter("MMM dd uuuu");
    private static final DateTimeFormatter DISPLAY_DATE_TIME = formatter("MMM dd uuuu h:mm a");
    private static final DateTimeFormatter STORAGE_DATE_TIME = formatter("uuuu-MM-dd HHmm");

    /**
     * Prevents instantiation of this utility class.
     */
    private TaskDateTime() {
    }

    /**
     * Parses a date, with an optional time, from a supported command or storage value.
     *
     * @param value date in {@code yyyy-MM-dd}, {@code yyyy-MM-dd HHmm}, or
     *              {@code d/M/yyyy HHmm} format
     * @return the parsed value; a date without a time is represented as midnight
     * @throws DateTimeParseException if the value is not a real date in a supported format
     */
    public static LocalDateTime parse(String value) {
        try {
            return LocalDate.parse(value, ISO_DATE).atStartOfDay();
        } catch (DateTimeParseException dateException) {
            for (DateTimeFormatter formatter : DATE_TIME_INPUT_FORMATTERS) {
                try {
                    return LocalDateTime.parse(value, formatter);
                } catch (DateTimeParseException dateTimeException) {
                    // Try the next supported format.
                }
            }
            throw new DateTimeParseException(
                    "Date must use yyyy-MM-dd or d/M/yyyy HHmm format", value, 0);
        }
    }

    /**
     * Formats a task date for display, including its time when one is present.
     *
     * @param dateTime date and optional time to format
     * @return a user-friendly date such as {@code Dec 02 2019 6:00 PM}
     */
    public static String formatForDisplay(LocalDateTime dateTime) {
        if (dateTime.toLocalTime().equals(LocalTime.MIDNIGHT)) {
            return dateTime.format(DISPLAY_DATE);
        }
        return dateTime.format(DISPLAY_DATE_TIME);
    }

    /**
     * Formats a task date in a stable representation suitable for saving and reloading.
     *
     * @param dateTime date and optional time to format
     * @return an ISO-style date with a compact 24-hour time when needed
     */
    public static String formatForStorage(LocalDateTime dateTime) {
        if (dateTime.toLocalTime().equals(LocalTime.MIDNIGHT)) {
            return dateTime.format(ISO_DATE);
        }
        return dateTime.format(STORAGE_DATE_TIME);
    }

    /**
     * Creates a locale-stable formatter for user-facing text.
     *
     * @param pattern date-time pattern used by the formatter
     * @return formatter that uses a consistent English locale
     */
    private static DateTimeFormatter formatter(String pattern) {
        return DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);
    }

    /**
     * Creates a strict formatter so impossible dates such as 31 February are rejected.
     *
     * @param pattern date-time pattern used by the formatter
     * @return formatter that rejects invalid date and time values
     */
    private static DateTimeFormatter strictFormatter(String pattern) {
        return formatter(pattern).withResolverStyle(ResolverStyle.STRICT);
    }
}
