package bola.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import bola.task.Deadline;
import bola.task.Event;
import bola.task.Task;
import bola.task.Todo;

/**
 * Checks the happy paths for saving tasks to and loading tasks from a data file.
 */
public class StorageTest {
    /**
     * Checks first-run loading, directory creation, serialization, loading, and empty overwrites.
     *
     * @throws Exception if temporary test files cannot be accessed.
     */
    @Test
    void testMissingFileAndRoundTrip() throws Exception {
        Path testDirectory = Files.createTempDirectory("bola-storage-test");
        Path dataFile = testDirectory.resolve("missing-directory").resolve("tasks.txt");
        Storage storage = new Storage(dataFile);

        assert storage.load().isEmpty();

        Todo todo = new Todo("read book");
        todo.markAsDone();
        Deadline deadline = new Deadline("return book", "2019-06-06");
        Event event = new Event("project meeting", "6/8/2019 1400", "6/8/2019 1600");

        storage.save(List.of(todo, deadline, event));

        List<String> savedLines = Files.readAllLines(dataFile);
        assert savedLines.equals(List.of(
                "T | 1 | read book",
                "D | 0 | return book | 2019-06-06",
                "E | 0 | project meeting | 2019-08-06 1400 | 2019-08-06 1600"));

        List<String> loadedTasks = storage.load().stream().map(Task::toDataString).toList();
        assert loadedTasks.equals(savedLines);

        storage.save(List.of());
        assert Files.readAllLines(dataFile).isEmpty();
        assert storage.load().isEmpty();
    }

    /**
     * Checks that vertical bars and backslashes survive a save-and-load round trip.
     *
     * @throws Exception if temporary test files cannot be accessed.
     */
    @Test
    void testEscapedCharacters() throws Exception {
        Path dataFile = Files.createTempDirectory("bola-escaping-test").resolve("tasks.txt");
        Storage storage = new Storage(dataFile);
        List<Task> tasks = List.of(
                new Todo("compare A | B"),
                new Deadline("open C:\\notes", "2019-01-04"),
                new Event("plan \\ review", "2019-01-04 1300", "2019-01-04 1400"));

        storage.save(tasks);

        List<String> expectedLines = tasks.stream().map(Task::toDataString).toList();
        List<String> loadedLines = storage.load().stream().map(Task::toDataString).toList();
        assert loadedLines.equals(expectedLines);
        assert expectedLines.get(0).equals("T | 0 | compare A \\| B");
        assert expectedLines.get(1).equals("D | 0 | open C:\\\\notes | 2019-01-04");
    }

    /**
     * Checks that harmless empty lines and a UTF-8 byte-order mark are ignored.
     *
     * @throws Exception if temporary test files cannot be accessed.
     */
    @Test
    void testBlankLines() throws Exception {
        Path dataFile = Files.createTempDirectory("bola-blank-line-test").resolve("tasks.txt");
        Files.write(dataFile, List.of("\uFEFFT | 0 | valid task", "", "   "));

        List<Task> tasks = new Storage(dataFile).load();

        assert tasks.size() == 1;
        assert tasks.get(0).toDataString().equals("T | 0 | valid task");
    }

    /**
     * Checks all structural validation rules for corrupted task records.
     *
     * @throws Exception if temporary test files cannot be accessed.
     */
    @Test
    void testMalformedRecords() throws Exception {
        Path dataFile = Files.createTempDirectory("bola-invalid-data-test").resolve("tasks.txt");
        assertLoadFails(dataFile, "X | 0 | unknown type",
                "Line 1 of the data file has an unknown task type: 'X'.");
        assertLoadFails(dataFile, "T | 2 | invalid status",
                "Line 1 of the data file has an invalid completion status; it must be 0 or 1.");
        assertLoadFails(dataFile, " | 0 | missing type",
                "Line 1 of the data file is missing its task type.");
        assertLoadFails(dataFile, "T | | missing status",
                "Line 1 of the data file is missing its completion status.");
        assertLoadFails(dataFile, "T | 0",
                "Line 1 of the data file has the wrong number of fields.");
        assertLoadFails(dataFile, "T | 0 | ",
                "Line 1 of the data file is missing its description.");
        assertLoadFails(dataFile, "T | 0 | task | extra field",
                "Line 1 of the data file has the wrong number of fields.");
        assertLoadFails(dataFile, "D | 0 | deadline | ",
                "Line 1 of the data file is missing its deadline.");
        assertLoadFails(dataFile, "E | 0 | event | start | ",
                "Line 1 of the data file is missing its end time.");
        assertLoadFails(dataFile, "E | 0 | event | | 2019-01-01",
                "Line 1 of the data file is missing its start time.");
        assertLoadFails(dataFile, "D | 0 | deadline | 2019-02-29",
                "Line 1 of the data file has an invalid date format.");
        assertLoadFails(dataFile, "E | 0 | event | 2019-01-01 | tomorrow",
                "Line 1 of the data file has an invalid date format.");

        Files.write(dataFile, List.of("T | 0 | valid", "X | 0 | invalid"));
        try {
            new Storage(dataFile).load();
            assert false : "Loading malformed data should fail";
        } catch (IOException exception) {
            assert exception.getMessage().equals(
                    "Line 2 of the data file has an unknown task type: 'X'.");
        }
    }

    /**
     * Checks that a directory cannot be mistaken for the data file.
     *
     * @throws Exception if temporary test files cannot be accessed.
     */
    @Test
    void testInvalidFilePath() throws Exception {
        Path directory = Files.createTempDirectory("bola-invalid-path-test");
        Storage storage = new Storage(directory);

        try {
            storage.load();
            assert false : "Loading from a directory should fail";
        } catch (IOException exception) {
            assert true;
        }

        try {
            storage.save(List.of(new Todo("task")));
            assert false : "Saving over a directory should fail";
        } catch (IOException exception) {
            assert true;
        }
    }

    /**
     * Checks that loading one malformed record produces a descriptive failure.
     *
     * @param dataFile temporary data file.
     * @param malformedRecord invalid record to test.
     * @param expectedMessage expected loading-failure explanation.
     * @throws Exception if temporary test files cannot be accessed.
     */
    private static void assertLoadFails(Path dataFile, String malformedRecord,
            String expectedMessage) throws Exception {
        Files.writeString(dataFile, malformedRecord);
        try {
            new Storage(dataFile).load();
            assert false : "Loading malformed data should fail: " + malformedRecord;
        } catch (IOException exception) {
            assert exception.getMessage().equals(expectedMessage);
        }
    }
}
