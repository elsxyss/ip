package bola.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

/**
 * Tests remote AI request construction and failure handling without network access.
 */
public class RemoteAiServiceTest {
    @Test
    void ask_question_sendsCommandReferenceAndReturnsTrimmedAnswer() throws AiException {
        ChatRequest[] capturedRequest = new ChatRequest[1];
        ChatModel model = new ChatModel() {
            @Override
            public ChatResponse doChat(ChatRequest request) {
                capturedRequest[0] = request;
                return ChatResponse.builder()
                        .aiMessage(AiMessage.from("  Use deadline DESCRIPTION /by DATE.  "))
                        .build();
            }
        };
        RemoteAiService service = new RemoteAiService(model);

        String answer = service.ask("How do I add a deadline?");

        assertEquals("Use deadline DESCRIPTION /by DATE.", answer);
        SystemMessage systemMessage = (SystemMessage) capturedRequest[0].messages().get(0);
        UserMessage userMessage = (UserMessage) capturedRequest[0].messages().get(1);
        assertTrue(systemMessage.text().contains("deadline <description> /by <date>"));
        assertTrue(systemMessage.text().contains("does not support task priorities"));
        assertEquals("How do I add a deadline?", userMessage.singleText());
    }

    @Test
    void ask_emptyOrFailedResponse_throwsAiException() {
        ChatModel emptyModel = new ChatModel() {
            @Override
            public ChatResponse doChat(ChatRequest request) {
                return ChatResponse.builder()
                        .aiMessage(AiMessage.from("   "))
                        .build();
            }
        };
        ChatModel failedModel = new ChatModel() {
            @Override
            public ChatResponse doChat(ChatRequest request) {
                throw new IllegalStateException("Remote service failed");
            }
        };

        assertThrows(AiException.class,
                () -> new RemoteAiService(emptyModel).ask("Question"));
        AiException failure = assertThrows(AiException.class,
                () -> new RemoteAiService(failedModel).ask("Question"));
        assertEquals("Remote service failed", failure.getCause().getMessage());
    }
}
