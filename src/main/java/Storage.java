import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
        this.filePath = Objects.requireNonNull(filePath);
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

        List<String> savedLines = Files.readAllLines(filePath);
        for (int i = 0; i < savedLines.size(); i++) {
            String taskData = savedLines.get(i);
            if (i == 0 && taskData.startsWith("\uFEFF")) {
                taskData = taskData.substring(1);
            }
            if (!taskData.isBlank()) {
                tasks.add(parseTask(taskData, i + 1));
            }
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
        Objects.requireNonNull(tasks);
        Path parentDirectory = filePath.getParent();
        if (parentDirectory == null) {
            parentDirectory = Path.of(".");
        }
        Files.createDirectories(parentDirectory);

        Path temporaryFile = Files.createTempFile(parentDirectory, "bola-", ".tmp");
        try {
            Files.write(temporaryFile, tasks.stream().map(Task::toDataString).toList());
            replaceDataFile(temporaryFile, filePath);
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    /**
     * Replaces the data file atomically when supported, with a portable fallback otherwise.
     *
     * @param temporaryFile fully written temporary file
     * @param destinationFile data file to replace
     * @throws IOException if the completed file cannot be moved into place
     */
    private void replaceDataFile(Path temporaryFile, Path destinationFile) throws IOException {
        try {
            Files.move(temporaryFile, destinationFile, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporaryFile, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Reconstructs one task from its saved representation.
     *
     * @param taskData one line from the data file
     * @param lineNumber one-based line number used in error messages
     * @return reconstructed task
     * @throws IOException if the line does not match Bola's data format
     */
    private Task parseTask(String taskData, int lineNumber) throws IOException {
        List<String> fields = splitFields(taskData);
        requireFieldCount(fields, 3, Integer.MAX_VALUE, lineNumber);
        requireNonBlank(fields.get(0), "task type", lineNumber);
        requireNonBlank(fields.get(1), "completion status", lineNumber);
        requireNonBlank(fields.get(2), "description", lineNumber);

        Task task;
        try {
            switch (fields.get(0)) {
            case "T":
                requireFieldCount(fields, 3, 3, lineNumber);
                task = new Todo(fields.get(2));
                break;
            case "D":
                requireFieldCount(fields, 4, 4, lineNumber);
                requireNonBlank(fields.get(3), "deadline", lineNumber);
                task = new Deadline(fields.get(2), fields.get(3));
                break;
            case "E":
                requireFieldCount(fields, 5, 5, lineNumber);
                requireNonBlank(fields.get(3), "start time", lineNumber);
                requireNonBlank(fields.get(4), "end time", lineNumber);
                task = new Event(fields.get(2), fields.get(3), fields.get(4));
                break;
            default:
                throw invalidData(lineNumber, "unknown task type '" + fields.get(0) + "'");
            }
        } catch (DateTimeParseException exception) {
            throw invalidData(lineNumber, "date must use yyyy-MM-dd or d/M/yyyy HHmm format");
        }

        if (fields.get(1).equals("1")) {
            task.markAsDone();
        } else if (!fields.get(1).equals("0")) {
            throw invalidData(lineNumber, "completion status must be 0 or 1");
        }
        return task;
    }

    /**
     * Splits a task record at unescaped vertical bars and restores escaped characters.
     *
     * @param taskData one line from the data file
     * @return parsed and trimmed fields
     */
    private List<String> splitFields(String taskData) {
        ArrayList<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();

        for (int i = 0; i < taskData.length(); i++) {
            char character = taskData.charAt(i);
            if (character == '\\' && i + 1 < taskData.length()) {
                char nextCharacter = taskData.charAt(i + 1);
                if (nextCharacter == '\\' || nextCharacter == '|') {
                    currentField.append(nextCharacter);
                    i++;
                    continue;
                }
            }
            if (character == '|') {
                fields.add(currentField.toString().strip());
                currentField.setLength(0);
            } else {
                currentField.append(character);
            }
        }
        fields.add(currentField.toString().strip());
        return fields;
    }

    /**
     * Checks that a saved task has an allowed number of fields.
     *
     * @param fields parsed task fields
     * @param minimumCount minimum allowed number of fields
     * @param maximumCount maximum allowed number of fields
     * @param lineNumber one-based line number used in error messages
     * @throws IOException if the field count is outside the allowed range
     */
    private void requireFieldCount(List<String> fields, int minimumCount, int maximumCount,
            int lineNumber) throws IOException {
        if (fields.size() < minimumCount || fields.size() > maximumCount) {
            throw invalidData(lineNumber, "incorrect number of fields");
        }
    }

    /**
     * Checks that a required task field contains visible text.
     *
     * @param field field to validate
     * @param fieldName field name used in the error message
     * @param lineNumber one-based line number used in error messages
     * @throws IOException if the field is blank
     */
    private void requireNonBlank(String field, String fieldName, int lineNumber)
            throws IOException {
        if (field.isBlank()) {
            throw invalidData(lineNumber, fieldName + " cannot be blank");
        }
    }

    /**
     * Creates a consistent exception for malformed saved data.
     *
     * @param lineNumber one-based line number containing the error
     * @param reason explanation of the malformed data
     * @return exception describing the invalid record
     */
    private IOException invalidData(int lineNumber, String reason) {
        return new IOException("Invalid task data on line " + lineNumber + ": " + reason + ".");
    }
}
