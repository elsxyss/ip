package bola.ai;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * Answers Bola help questions through a remote OpenAI-compatible language model.
 */
public class RemoteAiService implements AiService {
    private static final String API_KEY_ENVIRONMENT_VARIABLE = "LLM_API_KEY";
    private static final String BASE_URL = "https://api.groq.com/openai/v1";
    private static final String MODEL_NAME = "openai/gpt-oss-120b";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final String SYSTEM_PROMPT = """
            You are the help assistant for Bola, a task-management chatbot.
            Answer only questions about Bola's features and command syntax using the reference below.
            Do not claim that Bola supports features or commands absent from the reference.
            Do not execute or pretend to execute commands. Keep the answer concise, preferably one or two sentences.

            Available commands:
            1. todo <description> - adds an undated to-do.
            2. deadline <description> /by <date> - adds a deadline.
            3. event <description> /from <date> /to <date> - adds an event whose end follows its start.
            4. list - lists every task and its number.
            5. find <keyword> - finds tasks by a case-insensitive description match.
            6. upcoming <days> - lists dated tasks due within a positive number of days.
            7. mark <selection> - marks selected tasks as complete.
            8. unmark <selection> - marks selected tasks as incomplete.
            9. delete <selection> - deletes selected tasks, with confirmation for multiple tasks or all.
            10. help - shows the command summary.
            11. bye - exits Bola.
            12. @ai <question> - asks this AI for help about Bola.

            A selection can contain task numbers, inclusive ranges, or all. Separate numbers and ranges with
            spaces or commas. Dates use yyyy-MM-dd. A date and time can use yyyy-MM-dd HHmm or d/M/yyyy HHmm.
            Bola does not support task priorities.
            """;

    private final ChatModel model;

    /**
     * Creates a service backed by the supplied chat model.
     *
     * @param model remote chat model.
     */
    RemoteAiService(ChatModel model) {
        this.model = model;
    }

    /**
     * Creates the configured AI service when an API key is available.
     *
     * @return configured service, or an empty value when {@code LLM_API_KEY} is absent.
     */
    public static Optional<AiService> fromEnvironment() {
        String apiKey = System.getenv(API_KEY_ENVIRONMENT_VARIABLE);
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }

        ChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(BASE_URL)
                .modelName(MODEL_NAME)
                .timeout(REQUEST_TIMEOUT)
                .build();
        return Optional.of(new RemoteAiService(model));
    }

    @Override
    public String ask(String question) throws AiException {
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                        SystemMessage.from(SYSTEM_PROMPT),
                        UserMessage.from(question)))
                .build();
        try {
            ChatResponse response = model.chat(request);
            String answer = response.aiMessage().text().strip();
            if (answer.isEmpty()) {
                throw new AiException("The AI service returned an empty response");
            }
            return answer;
        } catch (RuntimeException exception) {
            throw new AiException(exception);
        }
    }
}
