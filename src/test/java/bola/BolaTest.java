package bola;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests the GUI response boundary and its shared command and persistence logic.
 */
public class BolaTest {
    @TempDir
    Path directory;

    @Test
    void getResponse_taskLifecycle_returnsPlainTextAndPersistsChanges() {
        String file = directory.resolve("bola.txt").toString();
        Bola bola = new Bola(file);
        assertEquals("Bola: Eh hello! I'm Bola.\nGot anything to settle today?", bola.getWelcome());
        assertEquals("Bola: Bo lah! Your task list is empty. 😌", bola.getResponse("list"));
        assertTrue(bola.getResponse("  todo read book  ").contains("[T][ ] read book"));
        assertTrue(bola.getResponse("mark 1").contains("[T][X] read book"));
        assertTrue(new Bola(file).getResponse("list").contains("[T][X] read book"));
        assertTrue(bola.getResponse("unmark 1").contains("[T][ ] read book"));
        assertTrue(bola.getResponse("find book").contains("[T][ ] read book"));
        assertTrue(bola.getResponse("delete 1").contains("No more tasks"));
        assertEquals("Bola: Bo lah! Your task list is empty. 😌", new Bola(file).getResponse("list"));
        assertFalse(bola.isExit());
        assertEquals("Bola: All settled? Steady lah. See you again! 👋", bola.getResponse("bye"));
        assertTrue(bola.isExit());
    }

    @Test
    void getResponse_datedTasksAndInvalidCommands_returnsResultsAndErrors() {
        Bola bola = new Bola(directory.resolve("bola.txt").toString());
        String today = LocalDate.now().toString();
        assertTrue(bola.getResponse("deadline submit /by " + today).contains("[D][ ] submit"));
        assertTrue(bola.getResponse("event meeting /from " + today + " /to " + today)
                .contains("[E][ ] meeting"));
        assertTrue(bola.getResponse("upcoming 7").contains("[D][ ] submit"));
        assertTrue(bola.getResponse("unknown").startsWith("Bola: Aiyo,"));
        assertTrue(bola.getResponse("mark 99").startsWith("Bola: Aiyo,"));
        assertTrue(bola.getResponse("todo").startsWith("Bola: Aiyo,"));
        assertTrue(bola.getResponse("list").contains("2. [E][ ] meeting"));
    }

    @Test
    void getResponse_massMarkAndUnmark_mutatesDistinctTasksInOriginalOrder() {
        String file = directory.resolve("bola.txt").toString();
        Bola bola = new Bola(file);
        addTodos(bola, 4);

        assertEquals("Bola: Nice, 4 tasks settled liao! ✅\n"
                        + "    1. [T][X] task 1\n"
                        + "    2. [T][X] task 2\n"
                        + "    3. [T][X] task 3\n"
                        + "    4. [T][X] task 4\n"
                        + "Now got 4 tasks in your list.",
                bola.getResponse("mark 1-3 2-4 2"));
        assertEquals("Bola: Okay, these 2 tasks not settled yet.\n"
                        + "    2. [T][ ] task 2\n"
                        + "    4. [T][ ] task 4\n"
                        + "Now got 4 tasks in your list.",
                bola.getResponse("unmark 4, 2"));
        assertTrue(new Bola(file).getResponse("list").contains("2. [T][ ] task 2"));
    }

    @Test
    void getResponse_massDelete_requiresConfirmationAndPersistsOnlyYes() throws IOException {
        Path file = directory.resolve("bola.txt");
        Bola bola = new Bola(file.toString());
        addTodos(bola, 4);
        String savedBeforeConfirmation = Files.readString(file);

        assertEquals("Bola: U sure u want to delete these 2 tasks? (Yes/No)",
                bola.getResponse("delete 2, 4"));
        assertEquals(savedBeforeConfirmation, Files.readString(file));
        assertEquals("Bola: Aiyo, please answer Yes or No, can?", bola.getResponse("list"));
        assertEquals("Bola: Okay, cancelled. No tasks changed.", bola.getResponse(" NO "));
        assertTrue(bola.getResponse("list").contains("4. [T][ ] task 4"));

        assertEquals("Bola: U sure u want to delete these 2 tasks? (Yes/No)",
                bola.getResponse("delete 2 4"));
        assertEquals("Bola: Okay, removed these 2 tasks already:\n"
                        + "    2. [T][ ] task 2\n"
                        + "    4. [T][ ] task 4\n"
                        + "Now got 2 tasks in your list.",
                bola.getResponse("YeS"));
        assertEquals("Bola: Your tasks all here:\n"
                        + "    1. [T][ ] task 1\n"
                        + "    2. [T][ ] task 3",
                new Bola(file.toString()).getResponse("list"));
    }

    @Test
    void getResponse_allAndInvalidMassSelections_followAtomicConfirmationRules() {
        Bola bola = new Bola(directory.resolve("bola.txt").toString());
        addTodos(bola, 3);

        assertEquals("Bola: Aiyo, task number 4 doesn't exist leh.",
                bola.getResponse("mark 1-4"));
        assertTrue(bola.getResponse("list").contains("1. [T][ ] task 1"));
        assertEquals("Bola: U sure u want to mark all 3 tasks? (Yes/No)",
                bola.getResponse("mark all"));
        assertTrue(bola.getResponse("yes").contains("3. [T][X] task 3"));
        assertEquals("Bola: U sure u want to delete all 3 tasks? (Yes/No)",
                bola.getResponse("delete all"));
        assertTrue(bola.getResponse("YES").contains("No more tasks in your list"));
        assertEquals("Bola: Aiyo, there are no tasks to unmark leh.",
                bola.getResponse("unmark all"));
    }

    @Test
    void getResponse_help_returnsDocumentedCommandsWithoutSaving() {
        Path file = directory.resolve("bola.txt");
        Bola bola = new Bola(file.toString());

        String response = bola.getResponse("help");

        assertTrue(response.startsWith("Bola: Can! Here are the commands:"));
        assertTrue(response.contains("    delete <selection>"));
        assertTrue(response.contains("Example: delete 1, 3-5 8"));
        assertFalse(Files.exists(file));
        assertEquals("Bola: Aiyo, I don't understand that command leh.",
                bola.getResponse("help delete"));
    }

    @Test
    void getResponse_unreadableStorage_warnsAndPreservesFile() throws IOException {
        Path file = directory.resolve("bola.txt");
        Files.writeString(file, "invalid saved data");
        Bola bola = new Bola(file.toString());
        assertTrue(bola.getWelcome().contains("I couldn't load your saved tasks"));
        bola.getResponse("todo read book");
        assertEquals("invalid saved data", Files.readString(file));
    }

    @Test
    void getResponse_saveFailure_warnsAndKeepsSessionUsable() throws IOException {
        Path parent = directory.resolve("data");
        Bola bola = new Bola(parent.resolve("bola.txt").toString());
        Files.writeString(parent, "blocks directory creation");
        assertTrue(bola.getResponse("todo read book").contains("I couldn't save your tasks"));
        assertTrue(bola.getResponse("list").contains("read book"));
        assertFalse(bola.getResponse("todo buy milk").contains("I couldn't save"));
    }

    /**
     * Adds numbered to-dos for mass-operation tests.
     */
    private void addTodos(Bola bola, int taskCount) {
        for (int i = 1; i <= taskCount; i++) {
            bola.getResponse("todo task " + i);
        }
    }
}
