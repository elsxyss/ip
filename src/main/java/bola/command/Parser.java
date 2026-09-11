package bola.command;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
     * Extracts and validates all task numbers in a task mutation command.
     *
     * <p>Numbers are returned as distinct zero-based indexes in their original list order.</p>
     *
     * @param input complete user input.
     * @param commandType task mutation command type.
     * @param taskCount number of tasks currently stored.
     * @return validated task selection.
     * @throws BolaException if the selection is missing, malformed, reversed, or out of range.
     */
    public TaskSelection parseTaskSelection(String input, CommandType commandType, int taskCount)
            throws BolaException {
        assert isTaskMutation(commandType) : "Only task mutation commands have task selections";
        assert taskCount >= 0 : "Task count cannot be negative";

        String command = commandType.getKeyword();
        assert input.equals(command) || input.startsWith(command + " ")
                : "Input must match the supplied command type";

        String selectionText = input.substring(command.length()).strip();
        if (selectionText.isEmpty()) {
            throw new BolaException("which task number you want me to " + command + "?");
        }
        if (selectionText.equals("all")) {
            if (taskCount == 0) {
                throw new BolaException("there are no tasks to " + command + " leh.");
            }
            return new TaskSelection(allTaskIndexes(taskCount), true);
        }
        if (containsAllKeyword(selectionText)) {
            throw new BolaException("all must be used by itself for " + command + ", can?");
        }

        String[] selectors = splitSelectors(selectionText, command);
        Set<Integer> taskIndexes = new TreeSet<>();
        for (String selector : selectors) {
            addSelectorIndexes(selector, command, taskCount, selectors.length, taskIndexes);
        }
        return new TaskSelection(new ArrayList<>(taskIndexes), false);
    }

    /**
     * Returns whether a command changes existing tasks by number.
     */
    private boolean isTaskMutation(CommandType commandType) {
        return commandType == CommandType.MARK || commandType == CommandType.UNMARK
                || commandType == CommandType.DELETE;
    }

    /**
     * Returns every valid zero-based task index.
     */
    private List<Integer> allTaskIndexes(int taskCount) {
        ArrayList<Integer> taskIndexes = new ArrayList<>();
        for (int i = 0; i < taskCount; i++) {
            taskIndexes.add(i);
        }
        return taskIndexes;
    }

    /**
     * Returns whether {@code all} occurs as a separate selector.
     */
    private boolean containsAllKeyword(String selectionText) {
        return List.of(selectionText.split("[\\s,]+"))
                .contains("all");
    }

    /**
     * Splits a selection after rejecting missing comma-separated items.
     */
    private String[] splitSelectors(String selectionText, String command) throws BolaException {
        if (selectionText.matches("^,.*") || selectionText.matches(".*,$")
                || selectionText.matches(".*,[\\s]*,.*")) {
            throw invalidTaskSelection(command, true);
        }
        return selectionText.split("[\\s,]+");
    }

    /**
     * Adds one number or inclusive range to the selected indexes.
     */
    private void addSelectorIndexes(String selector, String command, int taskCount,
            int selectorCount, Set<Integer> taskIndexes) throws BolaException {
        if (selector.matches("[+-]?\\d+")) {
            int taskNumber = parseTaskNumber(selector, command, selectorCount > 1);
            validateTaskNumber(taskNumber, selector, taskCount);
            taskIndexes.add(taskNumber - 1);
            return;
        }
        if (!selector.matches("\\d+-\\d+")) {
            throw invalidTaskSelection(command, selectorCount > 1 || selector.contains("-"));
        }

        String[] boundaries = selector.split("-", -1);
        int firstTaskNumber = parseTaskNumber(boundaries[0], command, true);
        int lastTaskNumber = parseTaskNumber(boundaries[1], command, true);
        if (firstTaskNumber > lastTaskNumber) {
            throw new BolaException("range " + selector
                    + " cannot leh; the start number must not be greater than the end number.");
        }
        validateTaskNumber(firstTaskNumber, boundaries[0], taskCount);
        if (lastTaskNumber > taskCount) {
            throw new BolaException("task number " + (taskCount + 1) + " doesn't exist leh.");
        }
        for (int taskNumber = firstTaskNumber; taskNumber <= lastTaskNumber; taskNumber++) {
            taskIndexes.add(taskNumber - 1);
        }
    }

    /**
     * Parses one integer and converts overflow into a user-facing selection error.
     */
    private int parseTaskNumber(String taskNumberText, String command, boolean isMassSyntax)
            throws BolaException {
        try {
            return Integer.parseInt(taskNumberText);
        } catch (NumberFormatException exception) {
            throw invalidTaskSelection(command, isMassSyntax);
        }
    }

    /**
     * Checks that a one-based task number refers to an existing task.
     */
    private void validateTaskNumber(int taskNumber, String taskNumberText, int taskCount)
            throws BolaException {
        if (taskNumber < 1 || taskNumber > taskCount) {
            throw new BolaException("task number " + taskNumberText + " doesn't exist leh.");
        }
    }

    /**
     * Creates the appropriate singular or mass selection error.
     */
    private BolaException invalidTaskSelection(String command, boolean isMassSyntax) {
        if (isMassSyntax) {
            return new BolaException("please give me valid task numbers or ranges to "
                    + command + ", can?");
        }
        return new BolaException("please give me a valid task number to " + command + ", can?");
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
        assert commandType == CommandType.UPCOMING
                : "Only the upcoming command has a day range";
        assert input.equals(commandType.getKeyword())
                || input.startsWith(commandType.getKeyword() + " ")
                : "Input must match the supplied command type";

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
