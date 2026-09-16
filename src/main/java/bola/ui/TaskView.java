package bola.ui;

/**
 * Contains the display information needed for one interactive GUI task row.
 *
 * @param number one-based task number used by mark and unmark commands.
 * @param typeName human-readable task type.
 * @param details task description and any scheduling details.
 * @param isDone whether the task is complete.
 */
public record TaskView(int number, String typeName, String details, boolean isDone) {
}
