package bola.ui;

import java.util.List;
import java.util.Scanner;

import bola.command.CommandType;
import bola.task.Task;

/**
 * Handles text input from and output to Bola's user.
 */
public class Ui {
    private static final String ORANGE_TEXT = "\u001B[38;2;217;72;0m";
    private static final String RESET_TEXT_COLOUR = "\u001B[0m";
    private static final String OUTER_DIVIDER =
            "================================================================";
    private static final String RESPONSE_DIVIDER =
            "    ____________________________________________________________";
    private static final String RESPONSE_INDENT = "     ";
    private static final String RESPONSE_ADDRESS = "Bola: ";
    private static final String ERROR_ADDRESS = "Bola: Aiyo, ";
    private static final String STORAGE_ERROR_ADDRESS = "Bola: Alamak, ";

    private final Scanner scanner;
    private StringBuilder response;

    /**
     * Creates a user interface that reads commands from standard input.
     */
    public Ui() {
        scanner = new Scanner(System.in);
    }

    /**
     * Returns whether another command is available from the user.
     *
     * @return true if another line can be read.
     */
    public boolean hasNextCommand() {
        return scanner.hasNextLine();
    }

    /**
     * Reads and trims the next command entered by the user.
     *
     * @return the next command.
     */
    public String readCommand() {
        return scanner.nextLine().strip();
    }

    /**
     * Shows Bola's greeting and any problem encountered while loading tasks.
     *
     * @param isStorageAvailable whether tasks were loaded successfully.
     * @param loadingFailureReason reason loading failed, if applicable.
     */
    public void showWelcome(boolean isStorageAvailable, String loadingFailureReason) {
        String banner = "    ____        __     \n"
                + "   / __ )____  / /___ _\n"
                + "  / __  / __ \\/ / __ `/\n"
                + " / /_/ / /_/ / / /_/ / \n"
                + "/_____/\\____/_/\\__,_/  \n";

        showLines(OUTER_DIVIDER, ORANGE_TEXT + banner + RESET_TEXT_COLOUR);
        showGreeting(isStorageAvailable, loadingFailureReason);
        showDivider();
    }

    /**
     * Shows the greeting and storage warning without the console banner.
     */
    public void showGreeting(boolean isStorageAvailable, String loadingFailureReason) {
        showLines(RESPONSE_INDENT + RESPONSE_ADDRESS + "Eh hello! I'm Bola.",
                RESPONSE_INDENT + "Got anything to settle today?");
        if (!isStorageAvailable) {
            showLines(RESPONSE_INDENT + STORAGE_ERROR_ADDRESS
                    + "I couldn't load your saved tasks. " + loadingFailureReason,
                    RESPONSE_INDENT
                    + "Don't worry—I won't overwrite your data file during this session.");
        }
    }

    /**
     * Shows Bola's farewell.
     */
    public void showGoodbye() {
        showLines(RESPONSE_INDENT + RESPONSE_ADDRESS
                + "All settled? Steady lah. See you again! 👋", OUTER_DIVIDER);
    }

    /**
     * Shows every supported command and the task-selection syntax.
     */
    public void showHelp() {
        showLines(RESPONSE_INDENT + RESPONSE_ADDRESS + "Can! Here are the commands:",
                RESPONSE_INDENT + "    todo <description>",
                RESPONSE_INDENT + "    deadline <description> /by <date>",
                RESPONSE_INDENT + "    event <description> /from <date> /to <date>",
                RESPONSE_INDENT + "    list",
                RESPONSE_INDENT + "    find <keyword>",
                RESPONSE_INDENT + "    upcoming <days>",
                RESPONSE_INDENT + "    mark <selection>",
                RESPONSE_INDENT + "    unmark <selection>",
                RESPONSE_INDENT + "    delete <selection>",
                RESPONSE_INDENT + "    help",
                RESPONSE_INDENT + "    bye",
                RESPONSE_INDENT + "Selection can use task numbers, inclusive ranges, or all.",
                RESPONSE_INDENT + "Example: delete 1, 3-5 8",
                RESPONSE_INDENT
                        + "Dates use yyyy-MM-dd, or d/M/yyyy HHmm when including a time.");
    }

    /**
     * Shows all tasks with their one-based task numbers.
     *
     * @param tasks tasks to show.
     */
    public void showTaskList(List<Task> tasks) {
        if (tasks.isEmpty()) {
            showLine(RESPONSE_INDENT + RESPONSE_ADDRESS
                    + "Bo lah! Your task list is empty. 😌");
            return;
        }

        showLine(RESPONSE_INDENT + RESPONSE_ADDRESS + "Your tasks all here:");
        for (int i = 0; i < tasks.size(); i++) {
            showLine(RESPONSE_INDENT + "    " + (i + 1) + ". " + tasks.get(i));
        }
    }

    /**
     * Shows tasks whose descriptions match a search keyword.
     *
     * @param matchingTasks tasks that matched the keyword.
     * @param keyword keyword used to find the tasks.
     */
    public void showMatchingTasks(List<Task> matchingTasks, String keyword) {
        if (matchingTasks.isEmpty()) {
            showLine(RESPONSE_INDENT + RESPONSE_ADDRESS
                    + "Bo lah! No tasks matching \"" + keyword + "\".");
            return;
        }

        showLine(RESPONSE_INDENT + RESPONSE_ADDRESS
                + "Can, found these matching tasks:");
        for (int i = 0; i < matchingTasks.size(); i++) {
            showLine(RESPONSE_INDENT + "    " + (i + 1) + ". "
                    + matchingTasks.get(i));
        }
    }

    /**
     * Shows dated tasks due within the requested number of days.
     *
     * @param upcomingTasks matching tasks in chronological order.
     * @param allTasks complete task list, used to preserve the displayed task numbers.
     * @param days number of days in the requested range.
     */
    public void showUpcomingTasks(List<Task> upcomingTasks, List<Task> allTasks, int days) {
        assert days > 0 : "Upcoming-task responses require a positive day range";

        String dayWord = days == 1 ? "day" : "days";
        if (upcomingTasks.isEmpty()) {
            showLine(RESPONSE_INDENT + RESPONSE_ADDRESS
                    + "Bo lah! No dated tasks coming up in the next "
                    + days + " " + dayWord + ". 😌");
            return;
        }

        showLine(RESPONSE_INDENT + RESPONSE_ADDRESS
                + "Next " + days + " " + dayWord + " got these tasks:");
        for (Task task : upcomingTasks) {
            int originalTaskNumber = allTasks.indexOf(task) + 1;
            assert originalTaskNumber > 0
                    : "Every upcoming task must belong to the complete task list";
            showLine(RESPONSE_INDENT + "    " + originalTaskNumber + ". " + task);
        }
    }

    /**
     * Shows confirmation that a task was marked as done.
     *
     * @param task task that was marked.
     */
    public void showTaskMarked(Task task) {
        showLines(RESPONSE_INDENT + RESPONSE_ADDRESS
                + "Nice, one task settled liao! ✅", RESPONSE_INDENT + "    " + task);
    }

    /**
     * Shows confirmation that a task was marked as not done.
     *
     * @param task task that was unmarked.
     */
    public void showTaskUnmarked(Task task) {
        showLines(RESPONSE_INDENT + RESPONSE_ADDRESS
                + "Okay, this one not settled yet.", RESPONSE_INDENT + "    " + task);
    }

    /**
     * Shows confirmation that several tasks were marked as done.
     *
     * @param markedTasks tasks after they were marked.
     * @param taskIndexes original zero-based task indexes.
     * @param taskCount number of tasks currently stored.
     */
    public void showTasksMarked(List<Task> markedTasks, List<Integer> taskIndexes, int taskCount) {
        showLine(RESPONSE_INDENT + RESPONSE_ADDRESS + "Nice, " + markedTasks.size()
                + " tasks settled liao! ✅");
        showNumberedTasks(markedTasks, taskIndexes);
        showTaskCount(taskCount);
    }

    /**
     * Shows confirmation that several tasks were marked as not done.
     *
     * @param unmarkedTasks tasks after they were unmarked.
     * @param taskIndexes original zero-based task indexes.
     * @param taskCount number of tasks currently stored.
     */
    public void showTasksUnmarked(List<Task> unmarkedTasks, List<Integer> taskIndexes,
            int taskCount) {
        showLine(RESPONSE_INDENT + RESPONSE_ADDRESS + "Okay, these " + unmarkedTasks.size()
                + " tasks not settled yet.");
        showNumberedTasks(unmarkedTasks, taskIndexes);
        showTaskCount(taskCount);
    }

    /**
     * Shows a newly added task and the updated task count.
     *
     * @param task task that was added.
     * @param taskCount number of tasks currently stored.
     */
    public void showTaskAdded(Task task, int taskCount) {
        showLines(RESPONSE_INDENT + RESPONSE_ADDRESS
                + "Can! I've added this task:", RESPONSE_INDENT + "    " + task);
        showTaskCount(taskCount);
    }

    /**
     * Shows a removed task and the updated task count.
     *
     * @param task task that was removed.
     * @param taskCount number of tasks remaining.
     */
    public void showTaskDeleted(Task task, int taskCount) {
        showLines(RESPONSE_INDENT + RESPONSE_ADDRESS + "Okay, removed already:",
                RESPONSE_INDENT + "    " + task);
        if (taskCount == 0) {
            showLine(RESPONSE_INDENT + "Bo lah! No more tasks in your list. 🎉");
        } else {
            showTaskCount(taskCount);
        }
    }

    /**
     * Shows several removed tasks and the updated task count.
     *
     * @param deletedTasks tasks that were removed.
     * @param taskIndexes original zero-based task indexes.
     * @param taskCount number of tasks remaining.
     */
    public void showTasksDeleted(List<Task> deletedTasks, List<Integer> taskIndexes,
            int taskCount) {
        showLine(RESPONSE_INDENT + RESPONSE_ADDRESS + "Okay, removed these "
                + deletedTasks.size() + " tasks already:");
        showNumberedTasks(deletedTasks, taskIndexes);
        if (taskCount == 0) {
            showLine(RESPONSE_INDENT + "Bo lah! No more tasks in your list. 🎉");
        } else {
            showTaskCount(taskCount);
        }
    }

    /**
     * Asks the user to confirm a pending mass operation.
     *
     * @param commandType operation awaiting confirmation.
     * @param taskCount number of selected tasks.
     * @param isAll whether the selection used {@code all}.
     */
    public void showMassOperationConfirmation(CommandType commandType, int taskCount,
            boolean isAll) {
        String taskWord = taskCount == 1 ? "task" : "tasks";
        String selection = isAll ? "all " + taskCount + " " + taskWord
                : "these " + taskCount + " " + taskWord;
        showLine(RESPONSE_INDENT + RESPONSE_ADDRESS + "U sure u want to "
                + commandType.getKeyword() + " " + selection + "? (Yes/No)");
    }

    /**
     * Shows that a pending mass operation was cancelled.
     */
    public void showOperationCancelled() {
        showLine(RESPONSE_INDENT + RESPONSE_ADDRESS + "Okay, cancelled. No tasks changed.");
    }

    /**
     * Shows that a pending operation still requires a yes-or-no answer.
     */
    public void showConfirmationAnswerError() {
        showLine(RESPONSE_INDENT + ERROR_ADDRESS + "please answer Yes or No, can?");
    }

    /**
     * Shows an invalid-command error.
     *
     * @param message explanation of the invalid command.
     */
    public void showError(String message) {
        showLine(RESPONSE_INDENT + ERROR_ADDRESS + message);
    }

    /**
     * Shows a failure to save tasks and explains its effect on the session.
     */
    public void showSavingError() {
        showLines(RESPONSE_INDENT + STORAGE_ERROR_ADDRESS
                + "I couldn't save your tasks.", RESPONSE_INDENT
                + "Any more changes in this session won't be saved, okay?");
    }

    /**
     * Shows the divider between two responses.
     */
    public void showDivider() {
        showLine(RESPONSE_DIVIDER);
    }

    /**
     * Shows the task count using singular wording only when exactly one task remains.
     *
     * @param taskCount number of tasks currently stored.
     */
    private void showTaskCount(int taskCount) {
        if (taskCount == 1) {
            showLine(RESPONSE_INDENT + "Now got 1 task in your list.");
        } else {
            showLine(RESPONSE_INDENT + "Now got " + taskCount
                    + " tasks in your list.");
        }
    }

    /**
     * Shows tasks with their original one-based task numbers.
     */
    private void showNumberedTasks(List<Task> selectedTasks, List<Integer> taskIndexes) {
        assert selectedTasks.size() == taskIndexes.size()
                : "Every displayed task must have an original task index";
        for (int i = 0; i < selectedTasks.size(); i++) {
            showLine(RESPONSE_INDENT + "    " + (taskIndexes.get(i) + 1) + ". "
                    + selectedTasks.get(i));
        }
    }

    /**
     * Collects a response for a GUI dialog without redirecting global console output.
     *
     * @param operation response-producing operation.
     * @return plain text with console indentation and dividers removed.
     */
    public String captureResponse(Runnable operation) {
        response = new StringBuilder();
        try {
            operation.run();
            return response.toString().stripTrailing();
        } finally {
            response = null;
        }
    }

    /**
     * Sends the supplied lines in order using the console or GUI response formatting.
     *
     * @param lines lines to display.
     */
    private void showLines(String... lines) {
        for (String line : lines) {
            showLine(line);
        }
    }

    /**
     * Sends one line to the console or the current GUI response.
     */
    private void showLine(String line) {
        if (response == null) {
            System.out.println(line);
        } else if (!line.equals(OUTER_DIVIDER) && !line.equals(RESPONSE_DIVIDER)) {
            String text = line.startsWith(RESPONSE_INDENT) ? line.substring(RESPONSE_INDENT.length()) : line;
            response.append(text).append("\n");
        }
    }
}
