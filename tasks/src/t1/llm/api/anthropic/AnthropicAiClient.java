package t1.llm.api.anthropic;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.MessageCreateParams;
import t1.llm.api.AiClient;
import commons.model.Message;
import commons.model.Role;

import java.util.List;

/**
 * Anthropic Claude client using the official Anthropic Java SDK.
 * <p>
 * Claude's API differs from OpenAI: the system prompt is a separate {@code system} parameter,
 * not a message in the conversation. Max tokens must always be specified.
 * Compare with {@link CustomAnthropicAiClient} for the raw HTTP equivalent.
 */
public class AnthropicAiClient extends AiClient {

    private final AnthropicClient client;

    public AnthropicAiClient(String endpoint, String modelName,
                              String apiKey, String systemPrompt) {
        super(endpoint, modelName, apiKey, systemPrompt);
        this.client = AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }

    @Override
    public Message response(List<Message> messages) {
        var params = buildParams(messages);
        var resp = client.messages().create(params);
        String content = resp.content().stream()
                .filter(com.anthropic.models.messages.ContentBlock::isText)
                .map(block -> block.asText().text())
                .reduce("", String::concat);
        System.out.println(content);
        return new Message(Role.ASSISTANT, content);
    }

    @Override
    public Message streamResponse(List<Message> messages) {
        var params = buildParams(messages);
        var sb = new StringBuilder();
        try (var stream = client.messages().createStreaming(params)) {
            stream.stream()
                    .filter(com.anthropic.models.messages.RawMessageStreamEvent::isContentBlockDelta)
                    .forEach(event -> {
                        var delta = event.asContentBlockDelta().delta();
                        if (delta.isText()) {
                            String text = delta.asText().text();
                            System.out.print(text);
                            sb.append(text);
                        }
                    });
        }
        System.out.println();
        return new Message(Role.ASSISTANT, sb.toString());
    }

    private MessageCreateParams buildParams(List<Message> messages) {
        var builder = MessageCreateParams.builder()
                .model(modelName)
                .system(systemPrompt)
                .maxTokens(1024L);
        for (Message m : messages) {
            switch (m.role()) {
                case USER -> builder.addUserMessage(m.content());
                case ASSISTANT -> builder.addAssistantMessage(m.content());
                default -> { /* system messages not added to conversation history */ }
            }
        }
        return builder.build();
    }
}
