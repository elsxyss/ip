import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Owns Bola's task collection and provides operations that act on that collection.
 */
public class TaskList {
    private final ArrayList<Task> tasks;

    /**
     * Creates an empty task list.
     */
    public TaskList() {
        tasks = new ArrayList<>();
    }

    /**
     * Creates a task list containing a defensive copy of the supplied tasks.
     *
     * @param tasks initial tasks
     */
    public TaskList(List<Task> tasks) {
        this.tasks = new ArrayList<>(Objects.requireNonNull(tasks));
    }

    /**
     * Returns the number of tasks in the list.
     *
     * @return task count
     */
    public int size() {
        return tasks.size();
    }

    /**
     * Returns a read-only view of the tasks in their current order.
     *
     * @return unmodifiable task view
     */
    public List<Task> getTasks() {
        return Collections.unmodifiableList(tasks);
    }

    /**
     * Adds a task to the end of the list.
     *
     * @param task task to add
     */
    public void add(Task task) {
        tasks.add(Objects.requireNonNull(task));
    }

    /**
     * Removes and returns the task at the given zero-based index.
     *
     * @param index zero-based task index
     * @return removed task
     */
    public Task delete(int index) {
        return tasks.remove(index);
    }

    /**
     * Marks and returns the task at the given zero-based index.
     *
     * @param index zero-based task index
     * @return marked task
     */
    public Task mark(int index) {
        Task task = tasks.get(index);
        task.markAsDone();
        return task;
    }

    /**
     * Unmarks and returns the task at the given zero-based index.
     *
     * @param index zero-based task index
     * @return unmarked task
     */
    public Task unmark(int index) {
        Task task = tasks.get(index);
        task.markAsNotDone();
        return task;
    }

    /**
     * Finds dated tasks within an inclusive range and sorts them chronologically.
     *
     * <p>The returned list is independent, so the stored task order is unchanged.</p>
     *
     * @param today first date to include
     * @param days number of days ahead to include
     * @return matching dated tasks in chronological order
     */
    public List<Task> findUpcomingTasks(LocalDate today, int days) {
        LocalDate lastDate = today.plusDays(days);
        ArrayList<Task> upcomingTasks = new ArrayList<>();

        for (Task task : tasks) {
            LocalDate taskDate = getTaskDateTime(task).toLocalDate();
            if (!taskDate.isBefore(today) && !taskDate.isAfter(lastDate)) {
                upcomingTasks.add(task);
            }
        }
        upcomingTasks.sort(Comparator.comparing(TaskList::getTaskDateTime));
        return upcomingTasks;
    }

    /**
     * Returns the date used to order a dated task, or the latest possible date for a to-do.
     */
    private static LocalDateTime getTaskDateTime(Task task) {
        if (task instanceof Deadline deadline) {
            return deadline.getBy();
        }
        if (task instanceof Event event) {
            return event.getFrom();
        }
        return LocalDateTime.MAX;
    }
}
