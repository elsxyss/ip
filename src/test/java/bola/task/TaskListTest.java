package bola.task;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks task collection ownership and mutation operations.
 */
public class TaskListTest {
    /**
     * Runs task-list checks using Java assertions.
     *
     * @param args command-line arguments; not used
     */
    public static void main(String[] args) {
        testCollectionOwnership();
        testTaskOperations();
    }

    /**
     * Checks that callers cannot structurally modify the task collection.
     */
    private static void testCollectionOwnership() {
        ArrayList<Task> initialTasks = new ArrayList<>(List.of(new Todo("first")));
        TaskList tasks = new TaskList(initialTasks);
        initialTasks.clear();

        assert tasks.size() == 1 : "The constructor must copy its input list";
        try {
            tasks.getTasks().add(new Todo("unauthorised"));
            assert false : "The exposed task view must be unmodifiable";
        } catch (UnsupportedOperationException exception) {
            assert true;
        }
    }

    /**
     * Checks adding, marking, unmarking, and deleting tasks.
     */
    private static void testTaskOperations() {
        Task firstTask = new Todo("first");
        Task secondTask = new Todo("second");
        TaskList tasks = new TaskList();

        tasks.add(firstTask);
        tasks.add(secondTask);
        assert tasks.size() == 2;

        assert tasks.mark(0) == firstTask;
        assert firstTask.toDataString().equals("T | 1 | first");
        assert tasks.unmark(0) == firstTask;
        assert firstTask.toDataString().equals("T | 0 | first");

        assert tasks.delete(0) == firstTask;
        assert tasks.getTasks().equals(List.of(secondTask));
    }
}
