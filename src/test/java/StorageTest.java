import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Checks the happy path for saving tasks to a data file.
 */
public class StorageTest {
    /**
     * Runs the storage checks using Java assertions.
     *
     * @param args command-line arguments; not used
     * @throws Exception if temporary files cannot be created or read
     */
    public static void main(String[] args) throws Exception {
        Path testDirectory = Files.createTempDirectory("bola-storage-test");
        Path dataFile = testDirectory.resolve("missing-directory").resolve("tasks.txt");
        Storage storage = new Storage(dataFile);

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
    }
}
