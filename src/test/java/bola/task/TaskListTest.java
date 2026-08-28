package bola.task;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests task collection ownership, mutation, and upcoming-task selection.
 */
public class TaskListTest {
    /**
     * Checks that callers cannot structurally modify the task collection.
     */
    @Test
    void constructorAndGetTasks_externalMutation_doesNotChangeTaskList() {
        ArrayList<Task> initialTasks = new ArrayList<>(List.of(new Todo("first")));
        TaskList tasks = new TaskList(initialTasks);
        initialTasks.clear();

        assertEquals(1, tasks.size(), "The constructor must copy its input list");
        assertThrows(UnsupportedOperationException.class,
                () -> tasks.getTasks().add(new Todo("unauthorised")));
    }

    /**
     * Checks adding, marking, unmarking, and deleting tasks at different indexes.
     */
    @Test
    void taskOperations_validIndexes_mutateAndReturnExpectedTasks() {
        Task firstTask = new Todo("first");
        Task secondTask = new Todo("second");
        TaskList tasks = new TaskList();

        tasks.add(firstTask);
        tasks.add(secondTask);

        assertAll(
                () -> assertEquals(2, tasks.size()),
                () -> assertSame(secondTask, tasks.mark(1)),
                () -> assertEquals("T | 1 | second", secondTask.toDataString()),
                () -> assertSame(secondTask, tasks.unmark(1)),
                () -> assertEquals("T | 0 | second", secondTask.toDataString()),
                () -> assertSame(firstTask, tasks.delete(0)),
                () -> assertEquals(List.of(secondTask), tasks.getTasks()));
    }

    /**
     * Checks both inclusive date boundaries, chronological sorting, and exclusion rules.
     */
    @Test
    void findUpcomingTasks_mixedTasks_filtersAndSortsWithoutChangingStoredOrder() {
        LocalDate today = LocalDate.of(2026, 8, 28);
        Task boundaryDeadline = new Deadline("boundary", "2026-09-04");
        Task pastDeadline = new Deadline("past", "2026-08-27");
        Task upcomingDeadline = new Deadline("submit report", "2026-09-02");
        Task todayDeadline = new Deadline("due today", "2026-08-28");
        Task upcomingEvent = new Event(
                "consultation", "2026-08-30 1400", "2026-08-30 1500");
        Task outsideDeadline = new Deadline("outside range", "2026-09-05");
        Task todo = new Todo("undated task");
        List<Task> originalOrder = List.of(boundaryDeadline, pastDeadline, upcomingDeadline,
                todayDeadline, upcomingEvent, outsideDeadline, todo);
        TaskList tasks = new TaskList(originalOrder);

        List<Task> upcomingTasks = tasks.findUpcomingTasks(today, 7);

        assertAll(
                () -> assertEquals(List.of(
                        todayDeadline, upcomingEvent, upcomingDeadline, boundaryDeadline),
                        upcomingTasks),
                () -> assertEquals(originalOrder, tasks.getTasks(),
                        "Finding upcoming tasks must not reorder stored tasks"));
    }

    /**
     * Checks a zero-day range and lists with no matching dated tasks.
     */
    @Test
    void findUpcomingTasks_zeroDayOrNoDatedTasks_returnsExpectedList() {
        LocalDate today = LocalDate.of(2026, 8, 28);
        Task midnight = new Deadline("midnight", "2026-08-28");
        Task afternoon = new Event(
                "afternoon", "2026-08-28 1400", "2026-08-28 1500");
        Task tomorrow = new Deadline("tomorrow", "2026-08-29");
        TaskList datedTasks = new TaskList(List.of(afternoon, tomorrow, midnight));
        TaskList undatedTasks = new TaskList(List.of(new Todo("read")));

        assertAll(
                () -> assertEquals(List.of(midnight, afternoon),
                        datedTasks.findUpcomingTasks(today, 0)),
                () -> assertEquals(List.of(), undatedTasks.findUpcomingTasks(today, 7)),
                () -> assertEquals(List.of(), new TaskList().findUpcomingTasks(today, 7)));
    }

    /**
     * Checks null values are rejected at the collection boundary.
     */
    @Test
    void constructorAndAdd_nullTask_throwsNullPointerException() {
        TaskList tasks = new TaskList();

        assertAll(
                () -> assertThrows(NullPointerException.class, () -> new TaskList(null)),
                () -> assertThrows(NullPointerException.class, () -> tasks.add(null)));
    }
}
