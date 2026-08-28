import java.util.List;
import java.util.Scanner;

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
    private static final String ERROR_ADDRESS = "Bola doesn't understand: ";
    private static final String STORAGE_ERROR_ADDRESS = "Bola couldn't access storage: ";

    private final Scanner scanner;

    /**
     * Creates a user interface that reads commands from standard input.
     */
    public Ui() {
        scanner = new Scanner(System.in);
    }

    /**
     * Returns whether another command is available from the user.
     *
     * @return true if another line can be read
     */
    public boolean hasNextCommand() {
        return scanner.hasNextLine();
    }

    /**
     * Reads and trims the next command entered by the user.
     *
     * @return the next command
     */
    public String readCommand() {
        return scanner.nextLine().strip();
    }

    /**
     * Shows Bola's greeting and any problem encountered while loading tasks.
     *
     * @param storageAvailable whether tasks were loaded successfully
     * @param loadingFailureReason reason loading failed, if applicable
     */
    public void showWelcome(boolean storageAvailable, String loadingFailureReason) {
        String banner = "    ____        __     \n"
                + "   / __ )____  / /___ _\n"
                + "  / __  / __ \\/ / __ `/\n"
                + " / /_/ / /_/ / / /_/ / \n"
                + "/_____/\\____/_/\\__,_/  \n";

        System.out.println(OUTER_DIVIDER);
        System.out.println(ORANGE_TEXT + banner + RESET_TEXT_COLOUR);
        System.out.println(RESPONSE_INDENT + RESPONSE_ADDRESS + "Yo! I'm Bola.");
        System.out.println(RESPONSE_INDENT + "What are we working on today?");
        if (!storageAvailable) {
            System.out.println(RESPONSE_INDENT + STORAGE_ERROR_ADDRESS
                    + "I couldn't load your tasks. " + loadingFailureReason);
            System.out.println(RESPONSE_INDENT
                    + "The data file won't be overwritten during this session.");
        }
        showDivider();
    }

    /**
     * Shows Bola's farewell.
     */
    public void showGoodbye() {
        System.out.println(RESPONSE_INDENT + RESPONSE_ADDRESS + "See you again soon! Bye ~");
        System.out.println(OUTER_DIVIDER);
    }

    /**
     * Shows all tasks with their one-based task numbers.
     *
     * @param tasks tasks to show
     */
    public void showTaskList(List<Task> tasks) {
        System.out.println(RESPONSE_INDENT + RESPONSE_ADDRESS + "Here's your list ~");
        for (int i = 0; i < tasks.size(); i++) {
            System.out.println(RESPONSE_INDENT + "    " + (i + 1) + ". " + tasks.get(i));
        }
    }

    /**
     * Shows dated tasks due within the requested number of days.
     *
     * @param upcomingTasks matching tasks in chronological order
     * @param allTasks complete task list, used to preserve the displayed task numbers
     * @param days number of days in the requested range
     */
    public void showUpcomingTasks(List<Task> upcomingTasks, List<Task> allTasks, int days) {
        String dayWord = days == 1 ? "day" : "days";
        System.out.println(RESPONSE_INDENT + RESPONSE_ADDRESS
                + "Here are your tasks for the next " + days + " " + dayWord + " ~");
        if (upcomingTasks.isEmpty()) {
            System.out.println(RESPONSE_INDENT + "    No dated tasks are coming up.");
            return;
        }

        for (Task task : upcomingTasks) {
            int originalTaskNumber = allTasks.indexOf(task) + 1;
            System.out.println(RESPONSE_INDENT + "    " + originalTaskNumber + ". " + task);
        }
    }

    /**
     * Shows confirmation that a task was marked as done.
     *
     * @param task task that was marked
     */
    public void showTaskMarked(Task task) {
        System.out.println(RESPONSE_INDENT + RESPONSE_ADDRESS
                + "Nice! I've marked this task as done.");
        System.out.println(RESPONSE_INDENT + "    " + task);
    }

    /**
     * Shows confirmation that a task was marked as not done.
     *
     * @param task task that was unmarked
     */
    public void showTaskUnmarked(Task task) {
        System.out.println(RESPONSE_INDENT + RESPONSE_ADDRESS
                + "Okay, I've marked this task as not done.");
        System.out.println(RESPONSE_INDENT + "    " + task);
    }

    /**
     * Shows a newly added task and the updated task count.
     *
     * @param task task that was added
     * @param taskCount number of tasks currently stored
     */
    public void showTaskAdded(Task task, int taskCount) {
        System.out.println(RESPONSE_INDENT + "Bola added:");
        System.out.println(RESPONSE_INDENT + "    " + task);
        showTaskCount(taskCount);
    }

    /**
     * Shows a removed task and the updated task count.
     *
     * @param task task that was removed
     * @param taskCount number of tasks remaining
     */
    public void showTaskDeleted(Task task, int taskCount) {
        System.out.println(RESPONSE_INDENT + "Bola removed:");
        System.out.println(RESPONSE_INDENT + "    " + task);
        showTaskCount(taskCount);
    }

    /**
     * Shows an invalid-command error.
     *
     * @param message explanation of the invalid command
     */
    public void showError(String message) {
        System.out.println(RESPONSE_INDENT + ERROR_ADDRESS + message);
    }

    /**
     * Shows a failure to save tasks and explains its effect on the session.
     */
    public void showSavingError() {
        System.out.println(RESPONSE_INDENT + STORAGE_ERROR_ADDRESS
                + "I couldn't save your tasks. Further changes won't be saved "
                + "during this session.");
    }

    /**
     * Shows the divider between two responses.
     */
    public void showDivider() {
        System.out.println(RESPONSE_DIVIDER);
    }

    /**
     * Shows the task count using singular wording only when exactly one task remains.
     *
     * @param taskCount number of tasks currently stored
     */
    private void showTaskCount(int taskCount) {
        if (taskCount == 1) {
            System.out.println(RESPONSE_INDENT + "There is now 1 task in your list!");
        } else {
            System.out.println(RESPONSE_INDENT + "There are now " + taskCount
                    + " tasks in your list!");
        }
    }
}
