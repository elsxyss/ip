package bola.ui;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import bola.task.Deadline;
import bola.task.Task;
import bola.task.Todo;

/**
 * Tests Bola's user-facing responses for successful, empty, and failed outcomes.
 */
public class UiTest {
    /**
     * Checks the normal greeting, storage warning, and farewell messages.
     */
    @Test
    void welcomeAndGoodbye_allStates_showSingaporeanPersona() {
        Ui ui = new Ui();

        String availableOutput = captureOutput(() -> ui.showWelcome(true, ""));
        String unavailableOutput = captureOutput(
                () -> ui.showWelcome(false, "Permission denied."));
        String goodbyeOutput = captureOutput(ui::showGoodbye);

        assertAll(
                () -> assertTrue(availableOutput.contains(
                        "Bola: Eh hello! I'm Bola." + System.lineSeparator()
                                + "     Got anything to settle today?")),
                () -> assertTrue(availableOutput.contains("Got anything to settle today?")),
                () -> assertTrue(unavailableOutput.contains(
                        "Bola: Alamak, I couldn't load your saved tasks. Permission denied.")),
                () -> assertTrue(unavailableOutput.contains(
                        "Don't worry—I won't overwrite your data file during this session.")),
                () -> assertTrue(goodbyeOutput.contains(
                        "Bola: All settled? Steady lah. See you again! 👋")));
    }

    /**
     * Checks populated and empty output for listing, searching, and upcoming tasks.
     */
    @Test
    void taskQueries_populatedAndEmptyResults_showContextualResponses() {
        Ui ui = new Ui();
        Task todo = new Todo("buy kopi");
        Task deadline = new Deadline("submit report", "2026-09-10");
        List<Task> allTasks = List.of(todo, deadline);

        String populatedList = captureOutput(() -> ui.showTaskList(allTasks));
        String emptyList = captureOutput(() -> ui.showTaskList(List.of()));
        String matchingTasks = captureOutput(
                () -> ui.showMatchingTasks(List.of(deadline), "report"));
        String noMatches = captureOutput(
                () -> ui.showMatchingTasks(List.of(), "exercise"));
        String upcomingTasks = captureOutput(
                () -> ui.showUpcomingTasks(List.of(deadline), allTasks, 7));
        String noUpcomingTask = captureOutput(
                () -> ui.showUpcomingTasks(List.of(), allTasks, 1));

        assertAll(
                () -> assertTrue(populatedList.contains("Bola: Your tasks all here:")),
                () -> assertTrue(populatedList.contains("1. [T][ ] buy kopi")),
                () -> assertTrue(emptyList.contains(
                        "Bola: Bo lah! Your task list is empty. 😌")),
                () -> assertTrue(matchingTasks.contains(
                        "Bola: Can, found these matching tasks:")),
                () -> assertTrue(noMatches.contains(
                        "Bola: Bo lah! No tasks matching \"exercise\".")),
                () -> assertTrue(upcomingTasks.contains(
                        "Bola: Next 7 days got these tasks:")),
                () -> assertTrue(noUpcomingTask.contains(
                        "Bola: Bo lah! No dated tasks coming up in the next 1 day. 😌")));
    }

    /**
     * Checks mutation confirmations and singular, plural, and empty task counts.
     */
    @Test
    void taskChanges_allCounts_showSingaporeanConfirmations() {
        Ui ui = new Ui();
        Task task = new Todo("buy kopi");
        task.markAsDone();

        String markedOutput = captureOutput(() -> ui.showTaskMarked(task));
        String unmarkedOutput = captureOutput(() -> ui.showTaskUnmarked(task));
        String oneTaskOutput = captureOutput(() -> ui.showTaskAdded(task, 1));
        String severalTasksOutput = captureOutput(() -> ui.showTaskAdded(task, 3));
        String deletedOutput = captureOutput(() -> ui.showTaskDeleted(task, 2));
        String emptyOutput = captureOutput(() -> ui.showTaskDeleted(task, 0));

        assertAll(
                () -> assertTrue(markedOutput.contains(
                        "Bola: Nice, one task settled liao! ✅")),
                () -> assertTrue(unmarkedOutput.contains(
                        "Bola: Okay, this one not settled yet.")),
                () -> assertTrue(oneTaskOutput.contains(
                        "Bola: Can! I've added this task:")),
                () -> assertTrue(oneTaskOutput.contains("Now got 1 task in your list.")),
                () -> assertTrue(severalTasksOutput.contains("Now got 3 tasks in your list.")),
                () -> assertTrue(deletedOutput.contains("Bola: Okay, removed already:")),
                () -> assertTrue(deletedOutput.contains("Now got 2 tasks in your list.")),
                () -> assertTrue(emptyOutput.contains(
                        "Bo lah! No more tasks in your list. 🎉")));
    }

    /**
     * Checks command and storage failure prefixes and the two-line saving warning.
     */
    @Test
    void failures_commandAndStorageProblems_showHelpfulResponses() {
        Ui ui = new Ui();

        String commandError = captureOutput(
                () -> ui.showError("I don't understand that command leh."));
        String savingError = captureOutput(ui::showSavingError);

        assertAll(
                () -> assertTrue(commandError.contains(
                        "Bola: Aiyo, I don't understand that command leh.")),
                () -> assertTrue(savingError.contains(
                        "Bola: Alamak, I couldn't save your tasks.")),
                () -> assertTrue(savingError.contains(
                        "Any more changes in this session won't be saved, okay?")));
    }

    /**
     * Captures console output produced by one UI operation.
     *
     * @param operation UI operation to invoke.
     * @return text printed by the operation.
     */
    private static String captureOutput(Runnable operation) {
        PrintStream originalOutput = System.out;
        ByteArrayOutputStream capturedBytes = new ByteArrayOutputStream();
        try (PrintStream capturedOutput = new PrintStream(
                capturedBytes, true, StandardCharsets.UTF_8)) {
            System.setOut(capturedOutput);
            operation.run();
        } finally {
            System.setOut(originalOutput);
        }
        return capturedBytes.toString(StandardCharsets.UTF_8);
    }
}
