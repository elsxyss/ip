import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Saves Bola's tasks to a file on the hard disk.
 */
public class Storage {
    private static final Path DEFAULT_FILE_PATH = Path.of("data", "bola.txt");

    private final Path filePath;

    /**
     * Creates storage that writes to Bola's default data file.
     */
    public Storage() {
        this(DEFAULT_FILE_PATH);
    }

    /**
     * Creates storage that writes to the specified file.
     *
     * @param filePath path of the data file
     */
    public Storage(Path filePath) {
        this.filePath = filePath;
    }

    /**
     * Replaces the data file with the current task list, creating its parent directory if needed.
     *
     * @param tasks tasks to save
     * @throws IOException if the tasks cannot be written
     */
    public void save(List<Task> tasks) throws IOException {
        Path parentDirectory = filePath.getParent();
        if (parentDirectory != null) {
            Files.createDirectories(parentDirectory);
        }
        Files.write(filePath, tasks.stream().map(Task::toDataString).toList());
    }
}
