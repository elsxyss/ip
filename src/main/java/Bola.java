import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Coordinates Bola's user interface, task operations, and persistent storage.
 */
public class Bola {
    private static final String BY_SEPARATOR = " /by";
    private static final String FROM_SEPARATOR = " /from";
    private static final String TO_SEPARATOR = " /to";

    /**
     * Displays Bola's greeting and responds to commands until the user enters {@code bye}.
     *
     * @param args command-line arguments; not used
     */
    public static void main(String[] args) {
        Ui ui = new Ui();
        Storage storage = new Storage();
        ArrayList<Task> tasks = new ArrayList<>();
        boolean storageAvailable = true;
        String loadingFailureReason = "";
        try {
            tasks = storage.load();
        } catch (IOException exception) {
            storageAvailable = false;
            loadingFailureReason = exception.getMessage();
        }

        ui.showWelcome(storageAvailable, loadingFailureReason);

        while (ui.hasNextCommand()) {
            String command = ui.readCommand();
            boolean taskListChanged = false;

            try {
                CommandType commandType = CommandType.fromInput(command)
                        .orElseThrow(() -> new BolaException("Hmm?"));

                switch (commandType) {
                case BYE:
                    ui.showGoodbye();
                    return;
                case LIST:
                    ui.showTaskList(tasks);
                    break;
                case UPCOMING:
                    showUpcomingTasks(command, commandType, tasks, ui);
                    break;
                case MARK:
                    int taskIndexToMark = getTaskIndex(command, commandType, tasks.size());
                    tasks.get(taskIndexToMark).markAsDone();
                    taskListChanged = true;
                    ui.showTaskMarked(tasks.get(taskIndexToMark));
                    break;
                case UNMARK:
                    int taskIndexToUnmark = getTaskIndex(command, commandType, tasks.size());
                    tasks.get(taskIndexToUnmark).markAsNotDone();
                    taskListChanged = true;
                    ui.showTaskUnmarked(tasks.get(taskIndexToUnmark));
                    break;
                case DELETE:
                    int taskIndexToDelete = getTaskIndex(command, commandType, tasks.size());
                    Task removedTask = tasks.remove(taskIndexToDelete);
                    taskListChanged = true;
                    ui.showTaskDeleted(removedTask, tasks.size());
                    break;
                case TODO:
                    String description = getDescription(command, commandType, "ToDo");
                    Task todo = new Todo(description);
                    tasks.add(todo);
                    taskListChanged = true;
                    ui.showTaskAdded(todo, tasks.size());
                    break;
                case DEADLINE:
                    addDeadline(command, commandType, tasks, ui);
                    taskListChanged = true;
                    break;
                case EVENT:
                    addEvent(command, commandType, tasks, ui);
                    taskListChanged = true;
                    break;
                default:
                    throw new AssertionError("Unhandled command type: " + commandType);
                }
                if (taskListChanged && storageAvailable) {
                    storage.save(tasks);
                }
            } catch (BolaException exception) {
                ui.showError(exception.getMessage());
            } catch (IOException exception) {
                storageAvailable = false;
                ui.showSavingError();
            }
            ui.showDivider();
        }
    }

    /**
     * Extracts a required task description from a task command.
     *
     * @param input complete user input
     * @param commandType type of task command
     * @param taskType task type used in the error message
     * @return the non-empty task description
     * @throws BolaException if no description was supplied
     */
    private static String getDescription(String input, CommandType commandType, String taskType)
            throws BolaException {
        String command = commandType.getKeyword();
        String description = input.substring(command.length()).strip();
        if (description.isEmpty()) {
            throw new BolaException("What shall I add as your " + taskType + " task?");
        }
        return description;
    }

    /**
     * Extracts and validates the one-based task number in a command that targets a task.
     *
     * @param input complete user input
     * @param commandType command type, such as {@link CommandType#MARK}
     * @param taskCount number of tasks currently stored
     * @return the corresponding zero-based list index
     * @throws BolaException if the task number is missing, non-numeric, or out of range
     */
    private static int getTaskIndex(String input, CommandType commandType, int taskCount)
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
     * Prints dated tasks from today through the requested number of days ahead.
     *
     * @param input complete user input
     * @param commandType upcoming command type
     * @param tasks complete task list
     * @param ui user interface used to show the matching tasks
     * @throws BolaException if the number of days is missing or invalid
     */
    private static void showUpcomingTasks(String input, CommandType commandType,
            List<Task> tasks, Ui ui) throws BolaException {
        int days = getUpcomingDays(input, commandType);
        List<Task> upcomingTasks = findUpcomingTasks(tasks, LocalDate.now(), days);
        ui.showUpcomingTasks(upcomingTasks, tasks, days);
    }

    /**
     * Extracts the positive number of days supplied to the upcoming command.
     *
     * @param input complete user input
     * @param commandType upcoming command type
     * @return requested number of days
     * @throws BolaException if the value is missing, non-numeric, or not positive
     */
    private static int getUpcomingDays(String input, CommandType commandType)
            throws BolaException {
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
     * Finds deadlines due and events starting within an inclusive date range.
     *
     * <p>The returned list is sorted chronologically without changing the original task list.</p>
     *
     * @param tasks tasks to search
     * @param today first date to include
     * @param days number of days ahead to include
     * @return matching dated tasks in chronological order
     */
    static List<Task> findUpcomingTasks(List<Task> tasks, LocalDate today, int days) {
        LocalDate lastDate = today.plusDays(days);
        ArrayList<Task> upcomingTasks = new ArrayList<>();

        for (Task task : tasks) {
            LocalDate taskDate = getTaskDateTime(task).toLocalDate();
            if (!taskDate.isBefore(today) && !taskDate.isAfter(lastDate)) {
                upcomingTasks.add(task);
            }
        }
        upcomingTasks.sort(Comparator.comparing(Bola::getTaskDateTime));
        return upcomingTasks;
    }

    /**
     * Returns the date used to order a dated task, or the latest possible date for a to-do.
     */
    private static LocalDateTime getTaskDateTime(Task task) {
        if (task instanceof Deadline deadline) {
            return deadline.getBy();
        }
        if (task instanceof Event event) {
            return event.getFrom();
        }
        return LocalDateTime.MAX;
    }

    /**
     * Parses and adds a deadline task from a deadline command.
     *
     * @param input complete user input
     * @param commandType deadline command type
     * @param tasks list to which the new deadline is added
     * @param ui user interface used to confirm the addition
     * @throws BolaException if the description or deadline is missing
     */
    private static void addDeadline(String input, CommandType commandType, ArrayList<Task> tasks,
            Ui ui) throws BolaException {
        String taskDetails = input.substring(commandType.getKeyword().length()).strip();
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
        Task deadline = new Deadline(description, by);
        tasks.add(deadline);
        ui.showTaskAdded(deadline, tasks.size());
    }

    /**
     * Parses and adds an event task from an event command.
     *
     * @param input complete user input
     * @param commandType event command type
     * @param tasks list to which the new event is added
     * @param ui user interface used to confirm the addition
     * @throws BolaException if the description or time range is missing
     */
    private static void addEvent(String input, CommandType commandType, ArrayList<Task> tasks,
            Ui ui) throws BolaException {
        String taskDetails = input.substring(commandType.getKeyword().length()).strip();
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
        Task event = new Event(description, from, to);
        tasks.add(event);
        ui.showTaskAdded(event, tasks.size());
    }

    /**
     * Parses a task date and converts parsing failures into a helpful chatbot response.
     *
     * @param dateTimeText date and optional time entered by the user
     * @return parsed date and time
     * @throws BolaException if the date is invalid or uses an unsupported format
     */
    private static LocalDateTime parseTaskDateTime(String dateTimeText) throws BolaException {
        try {
            return TaskDateTime.parse(dateTimeText);
        } catch (DateTimeParseException exception) {
            throw new BolaException("Please enter dates as yyyy-MM-dd, or include a time as "
                    + "d/M/yyyy HHmm (e.g., 2/12/2019 1800).");
        }
    }

}
