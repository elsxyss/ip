package bola.command;

import java.util.Arrays;
import java.util.Optional;

/**
 * Represents a command that Bola can perform.
 */
public enum CommandType {
    /** Ends the current Bola session. */
    BYE("bye", false),
    /** Displays all saved tasks. */
    LIST("list", false),
    /** Displays dated tasks within a requested number of days. */
    UPCOMING("upcoming", true),
    /** Marks a task as completed. */
    MARK("mark", true),
    /** Marks a task as not completed. */
    UNMARK("unmark", true),
    /** Removes a task from the task list. */
    DELETE("delete", true),
    /** Adds a task without a date or time. */
    TODO("todo", true),
    /** Adds a task with a completion deadline. */
    DEADLINE("deadline", true),
    /** Adds a task with a start and end time. */
    EVENT("event", true);

    private final String keyword;
    private final boolean acceptsArguments;

    /**
     * Creates a command type with its user-facing keyword.
     *
     * @param keyword word used to invoke the command.
     * @param acceptsArguments whether the command may be followed by arguments.
     */
    CommandType(String keyword, boolean acceptsArguments) {
        this.keyword = keyword;
        this.acceptsArguments = acceptsArguments;
    }

    /**
     * Returns the keyword used to invoke this command.
     *
     * @return command keyword.
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * Identifies the command invoked by the given input.
     *
     * @param input complete user input.
     * @return the matching command type, or an empty value if the input is not a valid command.
     */
    public static Optional<CommandType> fromInput(String input) {
        return Arrays.stream(values())
                .filter(command -> command.matches(input))
                .findFirst();
    }

    /**
     * Returns whether the input invokes this command with a permitted argument format.
     *
     * @param input complete user input
     * @return true if the input invokes this command
     */
    private boolean matches(String input) {
        return input.equals(keyword) || acceptsArguments && input.startsWith(keyword + " ");
    }
}
