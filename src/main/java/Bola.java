/**
 * Greets the user when Bola starts and says goodbye before exiting.
 */
public class Bola {
    private static final String DIVIDER = "____________________________________________________________";

    /**
     * Displays Bola's greeting and farewell messages.
     *
     * @param args command-line arguments; not used
     */
    public static void main(String[] args) {
        String banner = "    ____        __     \n"
                + "   / __ )____  / /___ _\n"
                + "  / __  / __ \\/ / __ `/\n"
                + " / /_/ / /_/ / / /_/ / \n"
                + "/_____/\\____/_/\\__,_/  \n";

        System.out.println(DIVIDER);
        System.out.println(banner);
        System.out.println("Yo! I'm Bola.");
        System.out.println("What are we working on today?");
        System.out.println(DIVIDER);
        System.out.println("See you again soon! Bye ~");
        System.out.println(DIVIDER);
    }
}
