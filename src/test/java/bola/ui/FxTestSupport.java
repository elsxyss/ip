package bola.ui;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;

/**
 * Shares one JavaFX toolkit and runs UI assertions on its application thread.
 */
final class FxTestSupport {
    private static boolean isStarted;

    private FxTestSupport() {
    }

    static synchronized void start() throws Exception {
        if (!isStarted) {
            FutureTask<Void> startup = new FutureTask<>(() -> null);
            Platform.startup(startup);
            startup.get(10, TimeUnit.SECONDS);
            isStarted = true;
        }
    }

    static void run(Callable<Void> assertions) throws Exception {
        FutureTask<Void> task = new FutureTask<>(assertions);
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    static void run(Runnable assertions) throws Exception {
        run(() -> {
            assertions.run();
            return null;
        });
    }
}
