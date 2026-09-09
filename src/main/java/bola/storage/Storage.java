package bola.storage;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import bola.task.Deadline;
import bola.task.Event;
import bola.task.Task;
import bola.task.Todo;

/**
 * Saves and loads Bola's tasks using a file on the hard disk.
 */
public class Storage {
    private static final Path DEFAULT_FILE_PATH = Path.of("data", "bola.txt");
    private static final String BYTE_ORDER_MARK = "\uFEFF";

    private static final int TASK_TYPE_INDEX = 0;
    private static final int COMPLETION_STATUS_INDEX = 1;
    private static final int DESCRIPTION_INDEX = 2;
    private static final int START_OR_DEADLINE_INDEX = 3;
    private static final int END_TIME_INDEX = 4;

    private static final int TODO_FIELD_COUNT = 3;
    private static final int DEADLINE_FIELD_COUNT = 4;
    private static final int EVENT_FIELD_COUNT = 5;

    private static final String TODO_TYPE = "T";
    private static final String DEADLINE_TYPE = "D";
    private static final String EVENT_TYPE = "E";
    private static final String INCOMPLETE_STATUS = "0";
    private static final String COMPLETE_STATUS = "1";

    private static final char ESCAPE_CHARACTER = '\\';
    private static final char FIELD_SEPARATOR = '|';

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
     * @param filePath path of the data file.
     */
    public Storage(Path filePath) {
        this.filePath = Objects.requireNonNull(filePath);
    }

    /**
     * Loads the tasks in the data file, or returns an empty list if the file does not exist yet.
     *
     * @return tasks reconstructed from the data file.
     * @throws IOException if the data file cannot be read or contains an invalid task.
     */
    public ArrayList<Task> load() throws IOException {
        ArrayList<Task> tasks = new ArrayList<>();
        if (Files.notExists(filePath)) {
            return tasks;
        }

        List<String> savedLines = Files.readAllLines(filePath);
        for (int i = 0; i < savedLines.size(); i++) {
            String taskData = savedLines.get(i);
            if (i == 0 && taskData.startsWith(BYTE_ORDER_MARK)) {
                taskData = taskData.substring(BYTE_ORDER_MARK.length());
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
     * @param tasks tasks to save.
     * @throws IOException if the tasks cannot be written.
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
     * @param temporaryFile fully written temporary file.
     * @param destinationFile data file to replace.
     * @throws IOException if the completed file cannot be moved into place.
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
     * @param taskData one line from the data file.
     * @param lineNumber one-based line number used in error messages.
     * @return reconstructed task.
     * @throws IOException if the line does not match Bola's data format.
     */
    private Task parseTask(String taskData, int lineNumber) throws IOException {
        assert !taskData.isBlank() : "Only non-blank task records should be parsed";
        assert lineNumber > 0 : "Data-file line numbers are one-based";

        List<String> fields = splitFields(taskData);
        requireFieldCount(fields, TODO_FIELD_COUNT, Integer.MAX_VALUE, lineNumber);
        requireNonBlank(fields.get(TASK_TYPE_INDEX), "task type", lineNumber);
        requireNonBlank(fields.get(COMPLETION_STATUS_INDEX), "completion status", lineNumber);
        requireNonBlank(fields.get(DESCRIPTION_INDEX), "description", lineNumber);

        Task task = createTask(fields, lineNumber);
        restoreCompletionStatus(task, fields.get(COMPLETION_STATUS_INDEX), lineNumber);
        return task;
    }

    /**
     * Creates the task subtype identified by a saved record.
     *
     * @param fields parsed task fields.
     * @param lineNumber one-based line number used in error messages.
     * @return reconstructed task with its default incomplete status.
     * @throws IOException if the task type, field count, or date fields are invalid.
     */
    private Task createTask(List<String> fields, int lineNumber) throws IOException {
        try {
            return switch (fields.get(TASK_TYPE_INDEX)) {
                case TODO_TYPE -> createTodo(fields, lineNumber);
                case DEADLINE_TYPE -> createDeadline(fields, lineNumber);
                case EVENT_TYPE -> createEvent(fields, lineNumber);
                default -> throw invalidData(lineNumber,
                        "has an unknown task type: '"
                                + fields.get(TASK_TYPE_INDEX) + "'.");
            };
        } catch (DateTimeParseException exception) {
            throw invalidData(lineNumber, "has an invalid date format.");
        }
    }

    /**
     * Creates a to-do from validated common fields.
     */
    private Task createTodo(List<String> fields, int lineNumber) throws IOException {
        requireFieldCount(fields, TODO_FIELD_COUNT, TODO_FIELD_COUNT, lineNumber);
        return new Todo(fields.get(DESCRIPTION_INDEX));
    }

    /**
     * Creates a deadline after validating its subtype-specific fields.
     */
    private Task createDeadline(List<String> fields, int lineNumber) throws IOException {
        requireFieldCount(fields, DEADLINE_FIELD_COUNT, DEADLINE_FIELD_COUNT, lineNumber);
        requireNonBlank(fields.get(START_OR_DEADLINE_INDEX), "deadline", lineNumber);
        return new Deadline(fields.get(DESCRIPTION_INDEX),
                fields.get(START_OR_DEADLINE_INDEX));
    }

    /**
     * Creates an event after validating its subtype-specific fields.
     */
    private Task createEvent(List<String> fields, int lineNumber) throws IOException {
        requireFieldCount(fields, EVENT_FIELD_COUNT, EVENT_FIELD_COUNT, lineNumber);
        requireNonBlank(fields.get(START_OR_DEADLINE_INDEX), "start time", lineNumber);
        requireNonBlank(fields.get(END_TIME_INDEX), "end time", lineNumber);
        return new Event(fields.get(DESCRIPTION_INDEX),
                fields.get(START_OR_DEADLINE_INDEX), fields.get(END_TIME_INDEX));
    }

    /**
     * Restores and validates a reconstructed task's saved completion status.
     */
    private void restoreCompletionStatus(Task task, String completionStatus, int lineNumber)
            throws IOException {
        if (completionStatus.equals(COMPLETE_STATUS)) {
            task.markAsDone();
        } else if (!completionStatus.equals(INCOMPLETE_STATUS)) {
            throw invalidData(lineNumber,
                    "has an invalid completion status; it must be 0 or 1.");
        }
    }

    /**
     * Splits a task record at unescaped vertical bars and restores escaped characters.
     *
     * @param taskData one line from the data file.
     * @return parsed and trimmed fields.
     */
    private List<String> splitFields(String taskData) {
        ArrayList<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();

        for (int i = 0; i < taskData.length(); i++) {
            char character = taskData.charAt(i);
            if (character == ESCAPE_CHARACTER && i + 1 < taskData.length()) {
                char nextCharacter = taskData.charAt(i + 1);
                if (nextCharacter == ESCAPE_CHARACTER || nextCharacter == FIELD_SEPARATOR) {
                    currentField.append(nextCharacter);
                    i++;
                    continue;
                }
            }
            if (character == FIELD_SEPARATOR) {
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
     * @param fields parsed task fields.
     * @param minimumCount minimum allowed number of fields.
     * @param maximumCount maximum allowed number of fields.
     * @param lineNumber one-based line number used in error messages.
     * @throws IOException if the field count is outside the allowed range.
     */
    private void requireFieldCount(List<String> fields, int minimumCount, int maximumCount,
            int lineNumber) throws IOException {
        if (fields.size() < minimumCount || fields.size() > maximumCount) {
            throw invalidData(lineNumber, "has the wrong number of fields.");
        }
    }

    /**
     * Checks that a required task field contains visible text.
     *
     * @param field field to validate.
     * @param fieldName field name used in the error message.
     * @param lineNumber one-based line number used in error messages.
     * @throws IOException if the field is blank.
     */
    private void requireNonBlank(String field, String fieldName, int lineNumber)
            throws IOException {
        if (field.isBlank()) {
            throw invalidData(lineNumber, "is missing its " + fieldName + ".");
        }
    }

    /**
     * Creates a consistent exception for malformed saved data.
     *
     * @param lineNumber one-based line number containing the error.
     * @param reason explanation of the malformed data.
     * @return exception describing the invalid record.
     */
    private IOException invalidData(int lineNumber, String reason) {
        return new IOException("Line " + lineNumber + " of the data file " + reason);
    }
}
