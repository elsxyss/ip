package bola;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import bola.command.CommandType;
import bola.command.Parser;
import bola.command.TaskSelection;
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
    private PendingOperation pendingOperation;

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
            boolean taskListChanged;
            if (pendingOperation == null) {
                CommandType commandType = parser.parseCommandType(command);
                taskListChanged = executeCommand(command, commandType);
            } else {
                taskListChanged = handleConfirmation(command);
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
            case BYE -> exit();
            case HELP, LIST, FIND, UPCOMING -> executeReadOnlyCommand(command, commandType);
            case MARK, UNMARK, DELETE -> handleTaskMutation(command, commandType);
            case TODO, DEADLINE, EVENT -> createAndAddTask(command, commandType);
            default -> throw new AssertionError("Every command type must be handled explicitly");
        };
    }

    /**
     * Ends the current session after showing the farewell message.
     *
     * @return false because exiting does not change the task list.
     */
    private boolean exit() {
        ui.showGoodbye();
        isExit = true;
        return false;
    }

    /**
     * Executes a command that displays information without changing any task.
     *
     * @return false because read-only commands do not change the task list.
     */
    private boolean executeReadOnlyCommand(String command, CommandType commandType)
            throws BolaException {
        switch (commandType) {
            case HELP -> ui.showHelp();
            case LIST -> ui.showTaskList(tasks.getTasks());
            case FIND -> showMatchingTasks(command);
            case UPCOMING -> showUpcomingTasks(command, commandType);
            default -> throw new AssertionError("Only read-only commands can be executed here");
        }
        return false;
    }

    /**
     * Parses and adds a new task of the requested type.
     *
     * @return true because adding a task changes the task list.
     */
    private boolean createAndAddTask(String command, CommandType commandType) throws BolaException {
        Task task = switch (commandType) {
            case TODO -> parser.parseTodo(command);
            case DEADLINE -> parser.parseDeadline(command);
            case EVENT -> parser.parseEvent(command);
            default -> throw new AssertionError("Only task-creation commands can add tasks");
        };
        addTask(task);
        return true;
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
     * Validates a task selection and either executes it or requests confirmation.
     *
     * @return whether the operation changed the task list immediately.
     */
    private boolean handleTaskMutation(String command, CommandType commandType)
            throws BolaException {
        TaskSelection selection = parser.parseTaskSelection(command, commandType, tasks.size());
        if (requiresConfirmation(commandType, selection)) {
            pendingOperation = new PendingOperation(commandType, selection);
            ui.showMassOperationConfirmation(commandType, selection.taskIndexes().size(),
                    selection.isAll());
            return false;
        }
        executeTaskMutation(commandType, selection);
        return true;
    }

    /**
     * Returns whether a valid task mutation requires a yes-or-no response.
     */
    private boolean requiresConfirmation(CommandType commandType, TaskSelection selection) {
        boolean isMassDelete = commandType == CommandType.DELETE
                && selection.taskIndexes().size() > 1;
        return selection.isAll() || isMassDelete;
    }

    /**
     * Handles an answer to the currently pending mass operation.
     *
     * @return whether the confirmed operation changed the task list.
     */
    private boolean handleConfirmation(String answer) {
        if (answer.equalsIgnoreCase("no")) {
            pendingOperation = null;
            ui.showOperationCancelled();
            return false;
        }
        if (!answer.equalsIgnoreCase("yes")) {
            ui.showConfirmationAnswerError();
            return false;
        }

        PendingOperation confirmedOperation = pendingOperation;
        pendingOperation = null;
        executeTaskMutation(confirmedOperation.commandType(), confirmedOperation.selection());
        return true;
    }

    /**
     * Applies a validated selection and shows the matching single- or mass-task response.
     */
    private void executeTaskMutation(CommandType commandType, TaskSelection selection) {
        List<Integer> taskIndexes = selection.taskIndexes();
        if (taskIndexes.size() == 1) {
            executeSingleTaskMutation(commandType, taskIndexes.getFirst());
            return;
        }

        switch (commandType) {
            case MARK -> ui.showTasksMarked(tasks.mark(taskIndexes), taskIndexes, tasks.size());
            case UNMARK -> ui.showTasksUnmarked(tasks.unmark(taskIndexes), taskIndexes, tasks.size());
            case DELETE -> ui.showTasksDeleted(tasks.delete(taskIndexes), taskIndexes, tasks.size());
            default -> throw new AssertionError("Only task mutation commands can change selections");
        }
    }

    /**
     * Applies one selected task using the existing response format.
     */
    private void executeSingleTaskMutation(CommandType commandType, int taskIndex) {
        switch (commandType) {
            case MARK -> ui.showTaskMarked(tasks.mark(taskIndex));
            case UNMARK -> ui.showTaskUnmarked(tasks.unmark(taskIndex));
            case DELETE -> ui.showTaskDeleted(tasks.delete(taskIndex), tasks.size());
            default -> throw new AssertionError("Only task mutation commands can change selections");
        }
    }

    /**
     * Adds a task and shows its confirmation.
     */
    private void addTask(Task task) {
        tasks.add(task);
        ui.showTaskAdded(task, tasks.size());
    }

    /**
     * Holds a validated operation while Bola waits for confirmation.
     */
    private record PendingOperation(CommandType commandType, TaskSelection selection) {
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
