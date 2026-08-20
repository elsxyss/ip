import java.util.ArrayList;
import java.util.Scanner;

/**
 * Stores tasks entered by the user, lists them on request, and says goodbye on exit.
 */
public class Bola {
    private static final String ORANGE_TEXT = "\u001B[38;2;217;72;0m";
    private static final String RESET_TEXT_COLOUR = "\u001B[0m";
    private static final String OUTER_DIVIDER = "================================================================";
    private static final String RESPONSE_DIVIDER = "    ____________________________________________________________";
    private static final String RESPONSE_INDENT = "     ";
    private static final String RESPONSE_ADDRESS = "Bola: ";
    private static final String ERROR_ADDRESS = "Bola doesn't understand: ";
    private static final String EXIT_COMMAND = "bye";
    private static final String LIST_COMMAND = "list";
    private static final String MARK_COMMAND = "mark";
    private static final String UNMARK_COMMAND = "unmark";
    private static final String DELETE_COMMAND = "delete";
    private static final String TODO_COMMAND = "todo";
    private static final String DEADLINE_COMMAND = "deadline";
    private static final String EVENT_COMMAND = "event";
    private static final String BY_SEPARATOR = " /by";
    private static final String FROM_SEPARATOR = " /from";
    private static final String TO_SEPARATOR = " /to";

    /**
     * Displays Bola's greeting and responds to commands until the user enters {@code bye}.
     *
     * @param args command-line arguments; not used
     */
    public static void main(String[] args) {
        String banner = "    ____        __     \n"
                + "   / __ )____  / /___ _\n"
                + "  / __  / __ \\/ / __ `/\n"
                + " / /_/ / /_/ / / /_/ / \n"
                + "/_____/\\____/_/\\__,_/  \n";

        System.out.println(OUTER_DIVIDER);
        System.out.println(ORANGE_TEXT + banner + RESET_TEXT_COLOUR);
        System.out.println(RESPONSE_INDENT + RESPONSE_ADDRESS + "Yo! I'm Bola.");
        System.out.println(RESPONSE_INDENT + "What are we working on today?");
        System.out.println(RESPONSE_DIVIDER);

        Scanner scanner = new Scanner(System.in);
        ArrayList<Task> tasks = new ArrayList<>();

        while (scanner.hasNextLine()) {
            String command = scanner.nextLine().strip();

            if (command.equals(EXIT_COMMAND)) {
                System.out.println(RESPONSE_INDENT + RESPONSE_ADDRESS + "See you again soon! Bye ~");
                System.out.println(OUTER_DIVIDER);
                break;
            }

            try {
                if (command.equals(LIST_COMMAND)) {
                    System.out.println(RESPONSE_INDENT + RESPONSE_ADDRESS + "Here's your list ~");
                    for (int i = 0; i < tasks.size(); i++) {
                        System.out.println(RESPONSE_INDENT + "    " + (i + 1) + ". " + tasks.get(i));
                    }
                } else if (isCommand(command, MARK_COMMAND)) {
                    int taskIndex = getTaskIndex(command, MARK_COMMAND, tasks.size());
                    tasks.get(taskIndex).markAsDone();
                    System.out.println(RESPONSE_INDENT + RESPONSE_ADDRESS
                            + "Nice! I've marked this task as done.");
                    System.out.println(RESPONSE_INDENT + "    " + tasks.get(taskIndex));
                } else if (isCommand(command, UNMARK_COMMAND)) {
                    int taskIndex = getTaskIndex(command, UNMARK_COMMAND, tasks.size());
                    tasks.get(taskIndex).markAsNotDone();
                    System.out.println(RESPONSE_INDENT + RESPONSE_ADDRESS
                            + "Okay, I've marked this task as not done.");
                    System.out.println(RESPONSE_INDENT + "    " + tasks.get(taskIndex));
                } else if (isCommand(command, DELETE_COMMAND)) {
                    int taskIndex = getTaskIndex(command, DELETE_COMMAND, tasks.size());
                    Task removedTask = tasks.remove(taskIndex);
                    printTaskDeleted(removedTask, tasks.size());
                } else if (isCommand(command, TODO_COMMAND)) {
                    String description = getDescription(command, TODO_COMMAND, "ToDo");
                    Task task = new Todo(description);
                    tasks.add(task);
                    printTaskAdded(task, tasks.size());
                } else if (isCommand(command, DEADLINE_COMMAND)) {
                    String taskDetails = command.substring(DEADLINE_COMMAND.length()).strip();
                    if (taskDetails.isEmpty() || taskDetails.startsWith("/by")) {
                        throw new BolaException("What shall I add as your Deadline task?");
                    }
                    int bySeparatorIndex = taskDetails.indexOf(BY_SEPARATOR);
                    if (bySeparatorIndex < 0) {
                        throw new BolaException(
                                "When's your deadline for this task? (Please indicate with /by)");
                    }
                    String description = taskDetails.substring(0, bySeparatorIndex).strip();
                    String by = taskDetails.substring(bySeparatorIndex + BY_SEPARATOR.length()).strip();
                    if (description.isEmpty()) {
                        throw new BolaException("What shall I add as your Deadline task?");
                    }
                    if (by.isEmpty()) {
                        throw new BolaException(
                                "When's your deadline for this task? (Please indicate with /by)");
                    }
                    Task task = new Deadline(description, by);
                    tasks.add(task);
                    printTaskAdded(task, tasks.size());
                } else if (isCommand(command, EVENT_COMMAND)) {
                    String taskDetails = command.substring(EVENT_COMMAND.length()).strip();
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
                    String from = taskDetails.substring(
                            fromSeparatorIndex + FROM_SEPARATOR.length(), toSeparatorIndex).strip();
                    String to = taskDetails.substring(toSeparatorIndex + TO_SEPARATOR.length()).strip();
                    if (description.isEmpty()) {
                        throw new BolaException("What shall I add as your Event task?");
                    }
                    if (from.isEmpty() || to.isEmpty()) {
                        throw new BolaException(
                                "When's this event happening? (Please indicate with /from and /to)");
                    }
                    Task task = new Event(description, from, to);
                    tasks.add(task);
                    printTaskAdded(task, tasks.size());
                } else {
                    throw new BolaException("Hmm?");
                }
            } catch (BolaException exception) {
                System.out.println(RESPONSE_INDENT + ERROR_ADDRESS + exception.getMessage());
            }
            System.out.println(RESPONSE_DIVIDER);
        }
    }

    /**
     * Returns whether the input is a command keyword, optionally followed by arguments.
     *
     * @param input complete user input
     * @param command command keyword to match
     * @return true if the input invokes the command
     */
    private static boolean isCommand(String input, String command) {
        return input.equals(command) || input.startsWith(command + " ");
    }

    /**
     * Extracts a required task description from a task command.
     *
     * @param input complete user input
     * @param command task command keyword
     * @param taskType task type used in the error message
     * @return the non-empty task description
     * @throws BolaException if no description was supplied
     */
    private static String getDescription(String input, String command, String taskType)
            throws BolaException {
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
     * @param command command keyword, such as {@code mark}, {@code unmark}, or {@code delete}
     * @param taskCount number of tasks currently stored
     * @return the corresponding zero-based list index
     * @throws BolaException if the task number is missing, non-numeric, or out of range
     */
    private static int getTaskIndex(String input, String command, int taskCount)
            throws BolaException {
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
     * Prints a confirmation after a task has been added.
     *
     * @param task task that was added
     * @param taskCount number of tasks currently stored
     */
    private static void printTaskAdded(Task task, int taskCount) {
        System.out.println(RESPONSE_INDENT + "Bola added:");
        System.out.println(RESPONSE_INDENT + "    " + task);
        printTaskCount(taskCount);
    }

    /**
     * Prints the removed task and the grammatically correct number of remaining tasks.
     *
     * @param task task that was removed
     * @param taskCount number of tasks remaining
     */
    private static void printTaskDeleted(Task task, int taskCount) {
        System.out.println(RESPONSE_INDENT + "Bola removed:");
        System.out.println(RESPONSE_INDENT + "    " + task);
        printTaskCount(taskCount);
    }

    /**
     * Prints the task count using singular wording only when exactly one task remains.
     *
     * @param taskCount number of tasks currently stored
     */
    private static void printTaskCount(int taskCount) {
        if (taskCount == 1) {
            System.out.println(RESPONSE_INDENT + "There is now 1 task in your list!");
        } else {
            System.out.println(RESPONSE_INDENT + "There are now " + taskCount + " tasks in your list!");
        }
    }
}
