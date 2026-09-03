package bola.command;

import java.util.Arrays;
import java.util.Optional;

/**
 * Represents a command that Bola can perform.
 */
public enum CommandType {
    BYE("bye", false),
    LIST("list", false),
    FIND("find", true),
    UPCOMING("upcoming", true),
    MARK("mark", true),
    UNMARK("unmark", true),
    DELETE("delete", true),
    TODO("todo", true),
    DEADLINE("deadline", true),
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
     */
    private boolean matches(String input) {
        return input.equals(keyword) || acceptsArguments && input.startsWith(keyword + " ");
    }
}
