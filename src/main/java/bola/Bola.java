package bola;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import bola.command.CommandType;
import bola.command.Parser;
import bola.exception.BolaException;
import bola.storage.Storage;
import bola.task.Task;
import bola.task.TaskList;
import bola.ui.Ui;

/**
 * Coordinates Bola's user interface, task operations, and persistent storage.
 */
public class Bola {
    private final Storage storage;
    private final TaskList tasks;
    private final Ui ui;
    private final Parser parser;
    private final String loadingFailureReason;

    private boolean isStorageAvailable;

    /**
     * Creates Bola and loads tasks from the specified data file.
     *
     * <p>If loading fails, Bola starts with an empty task list and avoids overwriting the
     * inaccessible data file during this session.</p>
     *
     * @param filePath path of the data file
     */
    public Bola(String filePath) {
        storage = new Storage(Path.of(filePath));
        ui = new Ui();
        parser = new Parser();

        TaskList loadedTasks = new TaskList();
        boolean canUseStorage = true;
        String failureReason = "";
        try {
            loadedTasks = new TaskList(storage.load());
        } catch (IOException exception) {
            canUseStorage = false;
            failureReason = exception.getMessage();
        }
        tasks = loadedTasks;
        isStorageAvailable = canUseStorage;
        loadingFailureReason = failureReason;
    }

    /**
     * Displays Bola's greeting and responds to commands until the user enters {@code bye}.
     */
    public void run() {
        ui.showWelcome(isStorageAvailable, loadingFailureReason);

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
                case FIND:
                    String keyword = parser.parseFindKeyword(command);
                    ui.showMatchingTasks(tasks.findTasks(keyword), keyword);
                    break;
                case UPCOMING:
                    int days = parser.parseUpcomingDays(command, commandType);
                    showUpcomingTasks(days);
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
                if (taskListChanged && isStorageAvailable) {
                    storage.save(tasks.getTasks());
                }
            } catch (BolaException exception) {
                ui.showError(exception.getMessage());
            } catch (IOException exception) {
                isStorageAvailable = false;
                ui.showSavingError();
            }
            ui.showDivider();
        }
    }

    /**
     * Prints dated tasks from today through the requested number of days ahead.
     *
     * @param days number of days ahead to include
     */
    private void showUpcomingTasks(int days) {
        List<Task> upcomingTasks = tasks.findUpcomingTasks(LocalDate.now(), days);
        ui.showUpcomingTasks(upcomingTasks, tasks.getTasks(), days);
    }

    /**
     * Starts Bola using its default data-file location.
     *
     * @param args command-line arguments; not used
     */
    public static void main(String[] args) {
        new Bola("data/bola.txt").run();
    }
}
