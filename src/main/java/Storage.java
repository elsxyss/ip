import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Saves and loads Bola's tasks using a file on the hard disk.
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
     * Loads the tasks in the data file, or returns an empty list if the file does not exist yet.
     *
     * @return tasks reconstructed from the data file
     * @throws IOException if the data file cannot be read or contains an invalid task
     */
    public ArrayList<Task> load() throws IOException {
        ArrayList<Task> tasks = new ArrayList<>();
        if (Files.notExists(filePath)) {
            return tasks;
        }

        for (String taskData : Files.readAllLines(filePath)) {
            tasks.add(parseTask(taskData));
        }
        return tasks;
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

    /**
     * Reconstructs one task from its saved representation.
     *
     * @param taskData one line from the data file
     * @return reconstructed task
     * @throws IOException if the line does not match Bola's data format
     */
    private Task parseTask(String taskData) throws IOException {
        String[] fields = taskData.split("\\s*\\|\\s*", -1);
        if (fields.length < 3) {
            throw new IOException("Invalid task data: " + taskData);
        }

        Task task;
        switch (fields[0]) {
        case "T":
            requireFieldCount(fields, 3, taskData);
            task = new Todo(fields[2]);
            break;
        case "D":
            requireFieldCount(fields, 4, taskData);
            task = new Deadline(fields[2], fields[3]);
            break;
        case "E":
            requireFieldCount(fields, 5, taskData);
            task = new Event(fields[2], fields[3], fields[4]);
            break;
        default:
            throw new IOException("Invalid task data: " + taskData);
        }

        if (fields[1].equals("1")) {
            task.markAsDone();
        } else if (!fields[1].equals("0")) {
            throw new IOException("Invalid task data: " + taskData);
        }
        return task;
    }

    /**
     * Checks that a saved task has the expected number of fields.
     *
     * @param fields parsed task fields
     * @param expectedCount expected number of fields
     * @param taskData original saved task data
     * @throws IOException if the field count is incorrect
     */
    private void requireFieldCount(String[] fields, int expectedCount, String taskData)
            throws IOException {
        if (fields.length != expectedCount) {
            throw new IOException("Invalid task data: " + taskData);
        }
    }
}
