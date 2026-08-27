import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Checks the happy paths for saving tasks to and loading tasks from a data file.
 */
public class StorageTest {
    /**
     * Runs the storage checks using Java assertions.
     *
     * @param args command-line arguments; not used
     * @throws Exception if temporary files cannot be created or read
     */
    public static void main(String[] args) throws Exception {
        testMissingFileAndRoundTrip();
        testEscapedCharacters();
        testBlankLines();
        testMalformedRecords();
        testInvalidFilePath();
    }

    /**
     * Checks first-run loading, directory creation, serialization, loading, and empty overwrites.
     *
     * @throws Exception if temporary test files cannot be accessed
     */
    private static void testMissingFileAndRoundTrip() throws Exception {
        Path testDirectory = Files.createTempDirectory("bola-storage-test");
        Path dataFile = testDirectory.resolve("missing-directory").resolve("tasks.txt");
        Storage storage = new Storage(dataFile);

        assert storage.load().isEmpty();

        Todo todo = new Todo("read book");
        todo.markAsDone();
        Deadline deadline = new Deadline("return book", "June 6th");
        Event event = new Event("project meeting", "Aug 6th 2pm", "Aug 6th 4pm");

        storage.save(List.of(todo, deadline, event));

        List<String> savedLines = Files.readAllLines(dataFile);
        assert savedLines.equals(List.of(
                "T | 1 | read book",
                "D | 0 | return book | June 6th",
                "E | 0 | project meeting | Aug 6th 2pm | Aug 6th 4pm"));

        List<String> loadedTasks = storage.load().stream().map(Task::toDataString).toList();
        assert loadedTasks.equals(savedLines);

        storage.save(List.of());
        assert Files.readAllLines(dataFile).isEmpty();
        assert storage.load().isEmpty();
    }

    /**
     * Checks that vertical bars and backslashes survive a save-and-load round trip.
     *
     * @throws Exception if temporary test files cannot be accessed
     */
    private static void testEscapedCharacters() throws Exception {
        Path dataFile = Files.createTempDirectory("bola-escaping-test").resolve("tasks.txt");
        Storage storage = new Storage(dataFile);
        List<Task> tasks = List.of(
                new Todo("compare A | B"),
                new Deadline("open C:\\notes", "Friday | 5pm"),
                new Event("plan \\ review", "room A | 1pm", "room B | 2pm"));

        storage.save(tasks);

        List<String> expectedLines = tasks.stream().map(Task::toDataString).toList();
        List<String> loadedLines = storage.load().stream().map(Task::toDataString).toList();
        assert loadedLines.equals(expectedLines);
        assert expectedLines.get(0).equals("T | 0 | compare A \\| B");
        assert expectedLines.get(1).equals("D | 0 | open C:\\\\notes | Friday \\| 5pm");
    }

    /**
     * Checks that harmless empty lines and a UTF-8 byte-order mark are ignored.
     *
     * @throws Exception if temporary test files cannot be accessed
     */
    private static void testBlankLines() throws Exception {
        Path dataFile = Files.createTempDirectory("bola-blank-line-test").resolve("tasks.txt");
        Files.write(dataFile, List.of("\uFEFFT | 0 | valid task", "", "   "));

        List<Task> tasks = new Storage(dataFile).load();

        assert tasks.size() == 1;
        assert tasks.get(0).toDataString().equals("T | 0 | valid task");
    }

    /**
     * Checks all structural validation rules for corrupted task records.
     *
     * @throws Exception if temporary test files cannot be accessed
     */
    private static void testMalformedRecords() throws Exception {
        Path dataFile = Files.createTempDirectory("bola-invalid-data-test").resolve("tasks.txt");
        assertLoadFails(dataFile, "X | 0 | unknown type");
        assertLoadFails(dataFile, "T | 2 | invalid status");
        assertLoadFails(dataFile, "T | 0");
        assertLoadFails(dataFile, "T | 0 | ");
        assertLoadFails(dataFile, "T | 0 | task | extra field");
        assertLoadFails(dataFile, "D | 0 | deadline | ");
        assertLoadFails(dataFile, "E | 0 | event | start | ");

        Files.write(dataFile, List.of("T | 0 | valid", "X | 0 | invalid"));
        try {
            new Storage(dataFile).load();
            assert false : "Loading malformed data should fail";
        } catch (IOException exception) {
            assert exception.getMessage().contains("line 2");
        }
    }

    /**
     * Checks that a directory cannot be mistaken for the data file.
     *
     * @throws Exception if temporary test files cannot be accessed
     */
    private static void testInvalidFilePath() throws Exception {
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
     * @param dataFile temporary data file
     * @param malformedRecord invalid record to test
     * @throws Exception if temporary test files cannot be accessed
     */
    private static void assertLoadFails(Path dataFile, String malformedRecord) throws Exception {
        Files.writeString(dataFile, malformedRecord);
        try {
            new Storage(dataFile).load();
            assert false : "Loading malformed data should fail: " + malformedRecord;
        } catch (IOException exception) {
            assert exception.getMessage().contains("line 1");
        }
    }
}
