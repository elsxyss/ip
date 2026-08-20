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
    private static final String EXIT_COMMAND = "bye";
    private static final String LIST_COMMAND = "list";
    private static final String MARK_COMMAND = "mark ";
    private static final String UNMARK_COMMAND = "unmark ";
    private static final int MAX_TASKS = 100;

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
        Task[] tasks = new Task[MAX_TASKS];
        int taskCount = 0;

        while (scanner.hasNextLine()) {
            String command = scanner.nextLine();

            System.out.println(RESPONSE_DIVIDER);
            if (command.equals(EXIT_COMMAND)) {
                System.out.println(RESPONSE_INDENT + RESPONSE_ADDRESS + "See you again soon! Bye ~");
                System.out.println(OUTER_DIVIDER);
                break;
            }

            if (command.equals(LIST_COMMAND)) {
                System.out.println(RESPONSE_INDENT + RESPONSE_ADDRESS + "Here's your list ~");
                for (int i = 0; i < taskCount; i++) {
                    System.out.println(RESPONSE_INDENT + "    " + (i + 1) + ". " + tasks[i]);
                }
            } else if (command.startsWith(MARK_COMMAND)) {
                int taskIndex = Integer.parseInt(command.substring(MARK_COMMAND.length())) - 1;
                tasks[taskIndex].markAsDone();
                System.out.println(RESPONSE_INDENT + RESPONSE_ADDRESS
                        + "Nice! I've marked this task as done.");
                System.out.println(RESPONSE_INDENT + "    " + tasks[taskIndex]);
            } else if (command.startsWith(UNMARK_COMMAND)) {
                int taskIndex = Integer.parseInt(command.substring(UNMARK_COMMAND.length())) - 1;
                tasks[taskIndex].markAsNotDone();
                System.out.println(RESPONSE_INDENT + RESPONSE_ADDRESS
                        + "Okay, I've marked this task as not done.");
                System.out.println(RESPONSE_INDENT + "    " + tasks[taskIndex]);
            } else {
                tasks[taskCount] = new Task(command);
                taskCount++;
                System.out.println(RESPONSE_INDENT + "Bola added: " + command);
            }
            System.out.println(RESPONSE_DIVIDER);
        }
    }
}
