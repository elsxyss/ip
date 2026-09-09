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
    private boolean isExit;

    /**
     * Creates Bola and loads tasks from the specified data file.
     *
     * <p>If loading fails, Bola starts with an empty task list and avoids overwriting the
     * inaccessible data file during this session.</p>
     *
     * @param filePath path of the data file.
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

        while (!isExit && ui.hasNextCommand()) {
            executeCommand(ui.readCommand());
            if (!isExit) {
                ui.showDivider();
            }
        }
    }

    /**
     * Executes a chat command and returns plain text without writing to the console.
     *
     * @param input command entered in the GUI.
     * @return Bola's response.
     */
    public String getResponse(String input) {
        return ui.captureResponse(() -> executeCommand(input.strip()));
    }

    /**
     * Returns the GUI greeting, including any storage loading warning.
     */
    public String getWelcome() {
        return ui.captureResponse(() -> ui.showGreeting(isStorageAvailable, loadingFailureReason));
    }

    public boolean isExit() {
        return isExit;
    }

    /**
     * Processes one command using the same logic for console and graphical interfaces.
     */
    private void executeCommand(String command) {
        try {
            CommandType commandType = parser.parseCommandType(command);
            boolean taskListChanged = executeCommand(command, commandType);
            if (taskListChanged && isStorageAvailable) {
                storage.save(tasks.getTasks());
            }
        } catch (BolaException exception) {
            ui.showError(exception.getMessage());
        } catch (IOException exception) {
            isStorageAvailable = false;
            ui.showSavingError();
        }
    }

    /**
     * Executes a parsed command and reports whether it changed the task list.
     *
     * @param command complete user input.
     * @param commandType parsed command type.
     * @return true if the task list changed.
     * @throws BolaException if the command arguments are invalid.
     */
    private boolean executeCommand(String command, CommandType commandType) throws BolaException {
        return switch (commandType) {
            case BYE -> {
                ui.showGoodbye();
                isExit = true;
                yield false;
            }
            case LIST -> {
                ui.showTaskList(tasks.getTasks());
                yield false;
            }
            case FIND -> {
                showMatchingTasks(command);
                yield false;
            }
            case UPCOMING -> {
                showUpcomingTasks(command, commandType);
                yield false;
            }
            case MARK -> {
                markTask(command, commandType);
                yield true;
            }
            case UNMARK -> {
                unmarkTask(command, commandType);
                yield true;
            }
            case DELETE -> {
                deleteTask(command, commandType);
                yield true;
            }
            case TODO -> {
                addTask(parser.parseTodo(command));
                yield true;
            }
            case DEADLINE -> {
                addTask(parser.parseDeadline(command));
                yield true;
            }
            case EVENT -> {
                addTask(parser.parseEvent(command));
                yield true;
            }
        };
    }

    /**
     * Shows tasks whose descriptions contain the command's search keyword.
     */
    private void showMatchingTasks(String command) throws BolaException {
        String keyword = parser.parseFindKeyword(command);
        ui.showMatchingTasks(tasks.findTasks(keyword), keyword);
    }

    /**
     * Shows dated tasks in the range requested by an upcoming command.
     */
    private void showUpcomingTasks(String command, CommandType commandType) throws BolaException {
        int days = parser.parseUpcomingDays(command, commandType);
        List<Task> upcomingTasks = tasks.findUpcomingTasks(LocalDate.now(), days);
        ui.showUpcomingTasks(upcomingTasks, tasks.getTasks(), days);
    }

    /**
     * Marks the task selected by the command as done.
     */
    private void markTask(String command, CommandType commandType) throws BolaException {
        int taskIndex = parser.parseTaskIndex(command, commandType, tasks.size());
        ui.showTaskMarked(tasks.mark(taskIndex));
    }

    /**
     * Marks the task selected by the command as not done.
     */
    private void unmarkTask(String command, CommandType commandType) throws BolaException {
        int taskIndex = parser.parseTaskIndex(command, commandType, tasks.size());
        ui.showTaskUnmarked(tasks.unmark(taskIndex));
    }

    /**
     * Deletes the task selected by the command.
     */
    private void deleteTask(String command, CommandType commandType) throws BolaException {
        int taskIndex = parser.parseTaskIndex(command, commandType, tasks.size());
        Task removedTask = tasks.delete(taskIndex);
        ui.showTaskDeleted(removedTask, tasks.size());
    }

    /**
     * Adds a task and shows its confirmation.
     */
    private void addTask(Task task) {
        tasks.add(task);
        ui.showTaskAdded(task, tasks.size());
    }

    /**
     * Starts Bola using its default data-file location.
     *
     * @param args command-line arguments; not used.
     */
    public static void main(String[] args) {
        new Bola("data/bola.txt").run();
    }
}
