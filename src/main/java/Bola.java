import java.util.Scanner;

/**
 * Greets the user, echoes commands, and says goodbye when the user exits.
 */
public class Bola {
    private static final String ORANGE_TEXT = "\u001B[38;2;217;72;0m";
    private static final String RESET_TEXT_COLOUR = "\u001B[0m";
    private static final String OUTER_DIVIDER = "================================================================";
    private static final String RESPONSE_DIVIDER = "    ____________________________________________________________";
    private static final String RESPONSE_INDENT = "     ";
    private static final String RESPONSE_ADDRESS = "Bola: ";
    private static final String EXIT_COMMAND = "bye";

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
        while (scanner.hasNextLine()) {
            String command = scanner.nextLine();

            System.out.println(RESPONSE_DIVIDER);
            if (command.equals(EXIT_COMMAND)) {
                System.out.println(RESPONSE_INDENT + RESPONSE_ADDRESS + "See you again soon! Bye ~");
                System.out.println(OUTER_DIVIDER);
                break;
            }

            System.out.println(RESPONSE_INDENT + RESPONSE_ADDRESS + command);
            System.out.println(RESPONSE_DIVIDER);
        }
    }
}
