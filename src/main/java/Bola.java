import java.io.IOException;
import java.time.LocalDate;
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
        TaskList tasks = new TaskList();
        boolean storageAvailable = true;
        String loadingFailureReason = "";
        try {
            tasks = new TaskList(storage.load());
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
                    ui.showTaskList(tasks.getTasks());
                    break;
                case UPCOMING:
                    int days = parser.parseUpcomingDays(command, commandType);
                    showUpcomingTasks(days, tasks, ui);
                    break;
                case MARK:
                    int taskIndexToMark = parser.parseTaskIndex(
                            command, commandType, tasks.size());
                    Task markedTask = tasks.mark(taskIndexToMark);
                    taskListChanged = true;
                    ui.showTaskMarked(markedTask);
                    break;
                case UNMARK:
                    int taskIndexToUnmark = parser.parseTaskIndex(
                            command, commandType, tasks.size());
                    Task unmarkedTask = tasks.unmark(taskIndexToUnmark);
                    taskListChanged = true;
                    ui.showTaskUnmarked(unmarkedTask);
                    break;
                case DELETE:
                    int taskIndexToDelete = parser.parseTaskIndex(
                            command, commandType, tasks.size());
                    Task removedTask = tasks.delete(taskIndexToDelete);
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
                    storage.save(tasks.getTasks());
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
    private static void showUpcomingTasks(int days, TaskList tasks, Ui ui) {
        List<Task> upcomingTasks = tasks.findUpcomingTasks(LocalDate.now(), days);
        ui.showUpcomingTasks(upcomingTasks, tasks.getTasks(), days);
    }
}
