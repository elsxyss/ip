package bola.task;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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
     * @param tasks initial tasks.
     */
    public TaskList(List<Task> tasks) {
        this.tasks = new ArrayList<>(Objects.requireNonNull(tasks));
    }

    /**
     * Returns the number of tasks in the list.
     *
     * @return task count.
     */
    public int size() {
        return tasks.size();
    }

    /**
     * Returns a read-only view of the tasks in their current order.
     *
     * @return unmodifiable task view.
     */
    public List<Task> getTasks() {
        return Collections.unmodifiableList(tasks);
    }

    /**
     * Adds a task to the end of the list.
     *
     * @param task task to add.
     */
    public void add(Task task) {
        tasks.add(Objects.requireNonNull(task));
    }

    /**
     * Removes and returns the task at the given zero-based index.
     *
     * @param index zero-based task index.
     * @return removed task.
     */
    public Task delete(int index) {
        assert index >= 0 && index < tasks.size()
                : "Task index must refer to an existing task";
        return tasks.remove(index);
    }

    /**
     * Deletes and returns the tasks at the supplied zero-based indexes.
     *
     * @param indexes distinct indexes in ascending order.
     * @return deleted tasks in original list order.
     */
    public List<Task> delete(List<Integer> indexes) {
        assertValidIndexes(indexes);
        List<Task> deletedTasks = indexes.stream().map(tasks::get).toList();
        for (int i = indexes.size() - 1; i >= 0; i--) {
            tasks.remove((int) indexes.get(i));
        }
        return deletedTasks;
    }

    /**
     * Marks and returns the task at the given zero-based index.
     *
     * @param index zero-based task index.
     * @return marked task.
     */
    public Task mark(int index) {
        assert index >= 0 && index < tasks.size()
                : "Task index must refer to an existing task";
        Task task = tasks.get(index);
        task.markAsDone();
        return task;
    }

    /**
     * Marks and returns the tasks at the supplied zero-based indexes.
     *
     * @param indexes distinct indexes in ascending order.
     * @return marked tasks in original list order.
     */
    public List<Task> mark(List<Integer> indexes) {
        assertValidIndexes(indexes);
        return indexes.stream().map(this::mark).toList();
    }

    /**
     * Unmarks and returns the task at the given zero-based index.
     *
     * @param index zero-based task index.
     * @return unmarked task.
     */
    public Task unmark(int index) {
        assert index >= 0 && index < tasks.size()
                : "Task index must refer to an existing task";
        Task task = tasks.get(index);
        task.markAsNotDone();
        return task;
    }

    /**
     * Unmarks and returns the tasks at the supplied zero-based indexes.
     *
     * @param indexes distinct indexes in ascending order.
     * @return unmarked tasks in original list order.
     */
    public List<Task> unmark(List<Integer> indexes) {
        assertValidIndexes(indexes);
        return indexes.stream().map(this::unmark).toList();
    }

    /**
     * Checks the preconditions of a parsed task selection.
     */
    private void assertValidIndexes(List<Integer> indexes) {
        assert indexes != null && !indexes.isEmpty() : "A task selection must not be empty";
        int previousIndex = -1;
        for (int index : indexes) {
            assert index > previousIndex && index < tasks.size()
                    : "Task indexes must be distinct, ascending, and valid";
            previousIndex = index;
        }
    }

    /**
     * Finds tasks whose descriptions contain the given keyword, ignoring letter case.
     *
     * <p>The matching tasks retain their order in the main task list.</p>
     *
     * @param keyword text to look for in task descriptions.
     * @return matching tasks in their original order.
     */
    public List<Task> findTasks(String keyword) {
        String normalisedKeyword = Objects.requireNonNull(keyword).toLowerCase(Locale.ROOT);
        return tasks.stream()
                .filter(task -> task.getDescription().toLowerCase(Locale.ROOT)
                        .contains(normalisedKeyword))
                .toList();
    }

    /**
     * Finds dated tasks within an inclusive range and sorts them chronologically.
     *
     * <p>The returned list is independent, so the stored task order is unchanged.</p>
     *
     * @param today first date to include.
     * @param days number of days ahead to include.
     * @return matching dated tasks in chronological order.
     */
    public List<Task> findUpcomingTasks(LocalDate today, int days) {
        assert today != null : "Upcoming-task searches require a starting date";
        assert days > 0 : "Upcoming-task searches require a positive day range";

        LocalDate lastDate = today.plusDays(days);
        return tasks.stream()
                .filter(task -> {
                    LocalDate taskDate = getTaskDateTime(task).toLocalDate();
                    return !taskDate.isBefore(today) && !taskDate.isAfter(lastDate);
                })
                .sorted(Comparator.comparing(TaskList::getTaskDateTime))
                .toList();
    }

    /**
     * Returns the date used to order a dated task, or the latest possible date for a to-do.
     *
     * @param task task whose ordering date is required.
     * @return deadline, event start time, or the latest possible date for an undated task.
     */
    private static LocalDateTime getTaskDateTime(Task task) {
        if (task instanceof Deadline deadline) {
            return deadline.getByDate();
        }
        if (task instanceof Event event) {
            return event.getStartDate();
        }
        return LocalDateTime.MAX;
    }
}
