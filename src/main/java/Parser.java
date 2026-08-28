import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * Interprets user commands and converts their arguments into values Bola can use.
 */
public class Parser {
    private static final String BY_SEPARATOR = " /by";
    private static final String FROM_SEPARATOR = " /from";
    private static final String TO_SEPARATOR = " /to";

    /**
     * Identifies the command requested by the user.
     *
     * @param input complete user input
     * @return matching command type
     * @throws BolaException if the input does not match a supported command
     */
    public CommandType parseCommandType(String input) throws BolaException {
        return CommandType.fromInput(input).orElseThrow(() -> new BolaException("Hmm?"));
    }

    /**
     * Extracts and validates the task number in a command that targets an existing task.
     *
     * @param input complete user input
     * @param commandType command type, such as {@link CommandType#MARK}
     * @param taskCount number of tasks currently stored
     * @return corresponding zero-based list index
     * @throws BolaException if the task number is missing, non-numeric, or out of range
     */
    public int parseTaskIndex(String input, CommandType commandType, int taskCount)
            throws BolaException {
        String command = commandType.getKeyword();
        String taskNumber = input.substring(command.length()).strip();
        if (taskNumber.isEmpty()) {
            throw new BolaException("Which task number shall I " + command + "?");
        }

        try {
            int taskIndex = Integer.parseInt(taskNumber) - 1;
            if (taskIndex < 0 || taskIndex >= taskCount) {
                throw new BolaException("There is no task numbered " + taskNumber + ".");
            }
            return taskIndex;
        } catch (NumberFormatException exception) {
            throw new BolaException("Please enter a valid task number to " + command + ".");
        }
    }

    /**
     * Extracts the positive number of days supplied to an upcoming command.
     *
     * @param input complete user input
     * @param commandType upcoming command type
     * @return requested number of days
     * @throws BolaException if the value is missing, non-numeric, or not positive
     */
    public int parseUpcomingDays(String input, CommandType commandType) throws BolaException {
        String daysText = input.substring(commandType.getKeyword().length()).strip();
        if (daysText.isEmpty()) {
            throw new BolaException("How many days ahead shall I check? (e.g., upcoming 7)");
        }

        try {
            int days = Integer.parseInt(daysText);
            if (days <= 0) {
                throw new BolaException("Please enter a positive number of days.");
            }
            return days;
        } catch (NumberFormatException exception) {
            throw new BolaException("Please enter a whole number of days (e.g., upcoming 7).");
        }
    }

    /**
     * Creates a to-do from a validated to-do command.
     *
     * @param input complete user input
     * @return newly parsed to-do
     * @throws BolaException if no description was supplied
     */
    public Task parseTodo(String input) throws BolaException {
        return new Todo(parseDescription(input, CommandType.TODO, "ToDo"));
    }

    /**
     * Creates a deadline from a validated deadline command.
     *
     * @param input complete user input
     * @return newly parsed deadline
     * @throws BolaException if its description or deadline is missing or invalid
     */
    public Task parseDeadline(String input) throws BolaException {
        String taskDetails = input.substring(CommandType.DEADLINE.getKeyword().length()).strip();
        if (taskDetails.isEmpty() || taskDetails.startsWith("/by")) {
            throw new BolaException("What shall I add as your Deadline task?");
        }

        int bySeparatorIndex = taskDetails.indexOf(BY_SEPARATOR);
        if (bySeparatorIndex < 0) {
            throw new BolaException(
                    "When's your deadline for this task? (Please indicate with /by)");
        }

        String description = taskDetails.substring(0, bySeparatorIndex).strip();
        String byText = taskDetails.substring(bySeparatorIndex + BY_SEPARATOR.length()).strip();
        if (description.isEmpty()) {
            throw new BolaException("What shall I add as your Deadline task?");
        }
        if (byText.isEmpty()) {
            throw new BolaException(
                    "When's your deadline for this task? (Please indicate with /by)");
        }

        LocalDateTime by = parseTaskDateTime(byText);
        return new Deadline(description, by);
    }

    /**
     * Creates an event from a validated event command.
     *
     * @param input complete user input
     * @return newly parsed event
     * @throws BolaException if its description or time range is missing or invalid
     */
    public Task parseEvent(String input) throws BolaException {
        String taskDetails = input.substring(CommandType.EVENT.getKeyword().length()).strip();
        if (taskDetails.isEmpty() || taskDetails.startsWith("/from")) {
            throw new BolaException("What shall I add as your Event task?");
        }

        int fromSeparatorIndex = taskDetails.indexOf(FROM_SEPARATOR);
        int toSeparatorIndex = taskDetails.indexOf(TO_SEPARATOR,
                Math.max(0, fromSeparatorIndex + FROM_SEPARATOR.length()));
        if (fromSeparatorIndex < 0 || toSeparatorIndex < 0) {
            throw new BolaException(
                    "When's this event happening? (Please indicate with /from and /to)");
        }

        String description = taskDetails.substring(0, fromSeparatorIndex).strip();
        String fromText = taskDetails.substring(
                fromSeparatorIndex + FROM_SEPARATOR.length(), toSeparatorIndex).strip();
        String toText = taskDetails.substring(toSeparatorIndex + TO_SEPARATOR.length()).strip();
        if (description.isEmpty()) {
            throw new BolaException("What shall I add as your Event task?");
        }
        if (fromText.isEmpty() || toText.isEmpty()) {
            throw new BolaException(
                    "When's this event happening? (Please indicate with /from and /to)");
        }

        LocalDateTime from = parseTaskDateTime(fromText);
        LocalDateTime to = parseTaskDateTime(toText);
        return new Event(description, from, to);
    }

    /**
     * Extracts a required task description from a task command.
     *
     * @param input complete user input
     * @param commandType type of task command
     * @param taskType task type used in the error message
     * @return non-empty task description
     * @throws BolaException if no description was supplied
     */
    private String parseDescription(String input, CommandType commandType, String taskType)
            throws BolaException {
        String description = input.substring(commandType.getKeyword().length()).strip();
        if (description.isEmpty()) {
            throw new BolaException("What shall I add as your " + taskType + " task?");
        }
        return description;
    }

    /**
     * Parses a task date and converts parsing failures into a helpful chatbot response.
     *
     * @param dateTimeText date and optional time entered by the user
     * @return parsed date and time
     * @throws BolaException if the date is invalid or uses an unsupported format
     */
    private LocalDateTime parseTaskDateTime(String dateTimeText) throws BolaException {
        try {
            return TaskDateTime.parse(dateTimeText);
        } catch (DateTimeParseException exception) {
            throw new BolaException("Please enter dates as yyyy-MM-dd, or include a time as "
                    + "d/M/yyyy HHmm (e.g., 2/12/2019 1800).");
        }
    }
}
