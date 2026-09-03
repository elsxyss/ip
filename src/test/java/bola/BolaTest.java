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
}
