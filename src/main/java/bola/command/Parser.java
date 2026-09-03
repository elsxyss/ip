package bola.command;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import bola.exception.BolaException;
import bola.task.Deadline;
import bola.task.Event;
import bola.task.Task;
import bola.task.TaskDateTime;
import bola.task.Todo;

/**
 * Interprets user commands and converts their arguments into values Bola can use.
 */
public class Parser {
    private static final String BY_SEPARATOR = " /by";
    private static final String FROM_SEPARATOR = " /from";
    private static final String TO_SEPARATOR = " /to";

    /**
     * Creates a parser for interpreting Bola commands.
     */
    public Parser() {
    }

    /**
     * Identifies the command requested by the user.
     *
     * @param input complete user input.
     * @return matching command type.
     * @throws BolaException if the input does not match a supported command.
     */
    public CommandType parseCommandType(String input) throws BolaException {
        return CommandType.fromInput(input).orElseThrow(
                () -> new BolaException("I don't understand that command leh."));
    }

    /**
     * Extracts and validates the task number in a command that targets an existing task.
     *
     * @param input complete user input.
     * @param commandType command type, such as {@link CommandType#MARK}.
     * @param taskCount number of tasks currently stored.
     * @return corresponding zero-based list index.
     * @throws BolaException if the task number is missing, non-numeric, or out of range.
     */
    public int parseTaskIndex(String input, CommandType commandType, int taskCount)
            throws BolaException {
        String command = commandType.getKeyword();
        String taskNumber = input.substring(command.length()).strip();
        if (taskNumber.isEmpty()) {
            throw new BolaException("which task number you want me to " + command + "?");
        }

        try {
            int taskIndex = Integer.parseInt(taskNumber) - 1;
            if (taskIndex < 0 || taskIndex >= taskCount) {
                throw new BolaException("task number " + taskNumber + " doesn't exist leh.");
            }
            return taskIndex;
        } catch (NumberFormatException exception) {
            throw new BolaException("please give me a valid task number to " + command + ", can?");
        }
    }

    /**
     * Extracts the positive number of days supplied to an upcoming command.
     *
     * @param input complete user input.
     * @param commandType upcoming command type.
     * @return requested number of days.
     * @throws BolaException if the value is missing, non-numeric, or not positive.
     */
    public int parseUpcomingDays(String input, CommandType commandType) throws BolaException {
        String daysText = input.substring(commandType.getKeyword().length()).strip();
        if (daysText.isEmpty()) {
            throw new BolaException(
                    "how many days ahead should I check? Try upcoming 7.");
        }

        try {
            int days = Integer.parseInt(daysText);
            if (days <= 0) {
                throw new BolaException("the number of days must be positive, can?");
            }
            return days;
        } catch (NumberFormatException exception) {
            throw new BolaException(
                    "please use a whole number of days—for example, upcoming 7.");
        }
    }

    /**
     * Extracts the keyword supplied to a find command.
     *
     * @param input complete user input.
     * @return non-empty keyword to search for.
     * @throws BolaException if no keyword was supplied.
     */
    public String parseFindKeyword(String input) throws BolaException {
        String keyword = input.substring(CommandType.FIND.getKeyword().length()).strip();
        if (keyword.isEmpty()) {
            throw new BolaException("what keyword should I search for? Give me one, can?");
        }
        return keyword;
    }

    /**
     * Creates a to-do from a validated to-do command.
     *
     * @param input complete user input.
     * @return newly parsed to-do.
     * @throws BolaException if no description was supplied.
     */
    public Task parseTodo(String input) throws BolaException {
        return new Todo(parseDescription(input, CommandType.TODO, "to-do"));
    }

    /**
     * Creates a deadline from a validated deadline command.
     *
     * @param input complete user input.
     * @return newly parsed deadline.
     * @throws BolaException if its description or deadline is missing or invalid.
     */
    public Task parseDeadline(String input) throws BolaException {
        String taskDetails = input.substring(CommandType.DEADLINE.getKeyword().length()).strip();
        if (taskDetails.isEmpty() || taskDetails.startsWith("/by")) {
            throw new BolaException("what deadline task should I add for you?");
        }

        int bySeparatorIndex = taskDetails.indexOf(BY_SEPARATOR);
        if (bySeparatorIndex < 0) {
            throw new BolaException("when is this due? Use /by to tell me, can?");
        }

        String description = taskDetails.substring(0, bySeparatorIndex).strip();
        String byDateText = taskDetails.substring(
                bySeparatorIndex + BY_SEPARATOR.length()).strip();
        if (description.isEmpty()) {
            throw new BolaException("what deadline task should I add for you?");
        }
        if (byDateText.isEmpty()) {
            throw new BolaException("when is this due? Add a date after /by, can?");
        }

        LocalDateTime byDate = parseTaskDateTime(byDateText);
        return new Deadline(description, byDate);
    }

    /**
     * Creates an event from a validated event command.
     *
     * @param input complete user input.
     * @return newly parsed event.
     * @throws BolaException if its description or time range is missing or invalid.
     */
    public Task parseEvent(String input) throws BolaException {
        String taskDetails = input.substring(CommandType.EVENT.getKeyword().length()).strip();
        if (taskDetails.isEmpty() || taskDetails.startsWith("/from")) {
            throw new BolaException("what event should I add for you?");
        }

        int fromSeparatorIndex = taskDetails.indexOf(FROM_SEPARATOR);
        int toSeparatorIndex = taskDetails.indexOf(TO_SEPARATOR,
                Math.max(0, fromSeparatorIndex + FROM_SEPARATOR.length()));
        if (fromSeparatorIndex < 0 || toSeparatorIndex < 0) {
            throw new BolaException("when is this event happening? Use /from and /to, can?");
        }

        String description = taskDetails.substring(0, fromSeparatorIndex).strip();
        String startDateText = taskDetails.substring(
                fromSeparatorIndex + FROM_SEPARATOR.length(), toSeparatorIndex).strip();
        String endDateText = taskDetails.substring(
                toSeparatorIndex + TO_SEPARATOR.length()).strip();
        if (description.isEmpty()) {
            throw new BolaException("what event should I add for you?");
        }
        if (startDateText.isEmpty() || endDateText.isEmpty()) {
            throw new BolaException(
                    "I need both the start and end times. Use /from and /to, can?");
        }

        LocalDateTime startDate = parseTaskDateTime(startDateText);
        LocalDateTime endDate = parseTaskDateTime(endDateText);
        return new Event(description, startDate, endDate);
    }

    /**
     * Extracts a required task description from a task command.
     *
     * @param input complete user input.
     * @param commandType type of task command.
     * @param taskType task type used in the error message.
     * @return non-empty task description.
     * @throws BolaException if no description was supplied.
     */
    private String parseDescription(String input, CommandType commandType, String taskType)
            throws BolaException {
        String description = input.substring(commandType.getKeyword().length()).strip();
        if (description.isEmpty()) {
            throw new BolaException("what " + taskType + " should I add for you?");
        }
        return description;
    }

    /**
     * Parses a task date and converts parsing failures into a helpful chatbot response.
     *
     * @param dateTimeText date and optional time entered by the user.
     * @return parsed date and time.
     * @throws BolaException if the date is invalid or uses an unsupported format.
     */
    private LocalDateTime parseTaskDateTime(String dateTimeText) throws BolaException {
        try {
            return TaskDateTime.parse(dateTimeText);
        } catch (DateTimeParseException exception) {
            throw new BolaException("this date cannot leh. Use yyyy-MM-dd, or d/M/yyyy HHmm "
                    + "when including a time—for example, 2/12/2019 1800.");
        }
    }
}
