package bola.command;

import java.util.List;
import java.util.Objects;

/**
 * Represents validated task indexes selected by a task mutation command.
 *
 * @param taskIndexes distinct zero-based indexes in ascending list order.
 * @param isAll whether the user selected tasks with the {@code all} keyword.
 */
public record TaskSelection(List<Integer> taskIndexes, boolean isAll) {
    /**
     * Creates an immutable task selection.
     */
    public TaskSelection {
        taskIndexes = List.copyOf(Objects.requireNonNull(taskIndexes));
    }
}
