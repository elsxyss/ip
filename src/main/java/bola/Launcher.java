package bola;

import javafx.application.Application;

/**
 * Launches JavaFX without the classpath limitation of an Application entry point.
 */
public class Launcher {
    /**
     * Starts the GUI by default, or the original console when passed {@code --cli}.
     */
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--cli")) {
            Bola.main(args);
        } else {
            Application.launch(Main.class, args);
        }
    }
}
