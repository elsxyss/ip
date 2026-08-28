import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Coordinates Bola's user interface, task operations, and persistent storage.
 */
public class Bola {
    /**
     * Displays Bola's greeting and responds to commands until the user enters {@code bye}.
     *
     * @param args command-line arguments; not used
     */
    public static void main(String[] args) {
        Ui ui = new Ui();
        Parser parser = new Parser();
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
                CommandType commandType = parser.parseCommandType(command);

                switch (commandType) {
                case BYE:
                    ui.showGoodbye();
                    return;
                case LIST:
                    ui.showTaskList(tasks);
                    break;
                case UPCOMING:
                    int days = parser.parseUpcomingDays(command, commandType);
                    showUpcomingTasks(days, tasks, ui);
                    break;
                case MARK:
                    int taskIndexToMark = parser.parseTaskIndex(
                            command, commandType, tasks.size());
                    tasks.get(taskIndexToMark).markAsDone();
                    taskListChanged = true;
                    ui.showTaskMarked(tasks.get(taskIndexToMark));
                    break;
                case UNMARK:
                    int taskIndexToUnmark = parser.parseTaskIndex(
                            command, commandType, tasks.size());
                    tasks.get(taskIndexToUnmark).markAsNotDone();
                    taskListChanged = true;
                    ui.showTaskUnmarked(tasks.get(taskIndexToUnmark));
                    break;
                case DELETE:
                    int taskIndexToDelete = parser.parseTaskIndex(
                            command, commandType, tasks.size());
                    Task removedTask = tasks.remove(taskIndexToDelete);
                    taskListChanged = true;
                    ui.showTaskDeleted(removedTask, tasks.size());
                    break;
                case TODO:
                    Task todo = parser.parseTodo(command);
                    tasks.add(todo);
                    taskListChanged = true;
                    ui.showTaskAdded(todo, tasks.size());
                    break;
                case DEADLINE:
                    Task deadline = parser.parseDeadline(command);
                    tasks.add(deadline);
                    taskListChanged = true;
                    ui.showTaskAdded(deadline, tasks.size());
                    break;
                case EVENT:
                    Task event = parser.parseEvent(command);
                    tasks.add(event);
                    taskListChanged = true;
                    ui.showTaskAdded(event, tasks.size());
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
     * Prints dated tasks from today through the requested number of days ahead.
     *
     * @param days number of days ahead to include
     * @param tasks complete task list
     * @param ui user interface used to show the matching tasks
     */
    private static void showUpcomingTasks(int days, List<Task> tasks, Ui ui) {
        List<Task> upcomingTasks = findUpcomingTasks(tasks, LocalDate.now(), days);
        ui.showUpcomingTasks(upcomingTasks, tasks, days);
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

}
