package t1.llm.api.anthropic;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.RawContentBlockDelta;
import com.anthropic.models.messages.RawMessageStreamEvent;
import commons.exceptions.TaskNotImplementedException;
import commons.model.Message;
import commons.model.Role;
import java.util.List;
import t1.llm.api.AiClient;

/**
 * Anthropic Claude client using the official Anthropic Java SDK.
 * <p>
 * Claude's API differs from OpenAI: the system prompt is a separate {@code system} parameter,
 * not a message in the conversation. Max tokens must always be specified.
 * Compare with {@link CustomAnthropicAiClient} for the raw HTTP equivalent.
 */
public class AnthropicAiClient extends AiClient {

    private AnthropicClient client;

    public AnthropicAiClient(String endpoint, String modelName, String apiKey, String systemPrompt) {
        super(endpoint, modelName, apiKey, systemPrompt);
        //TODO:
        // - https://github.com/anthropics/anthropic-sdk-java
        // - Build an AnthropicClient using AnthropicOkHttpClient.builder(), set apiKey, and call build()
        // - Assign the result to this.client
        this.client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
    }

    @Override
    public Message response(List<Message> messages) {
        //TODO:
        // - Build MessageCreateParams using buildParams(messages)
        // - Call client.messages().create(params)
        // - Filter the response content blocks for isText(); extract text via asText().text(); concatenate
        // - Print content to stdout
        // - Return new Message(Role.ASSISTANT, content)
        MessageCreateParams messageCreateParams = buildParams(messages);
        com.anthropic.models.messages.Message message = client.messages().create(messageCreateParams);
        String content = message.content().stream().filter(ContentBlock::isText)
            .map(contentBlock -> contentBlock.asText().text()).reduce("", String::concat);
        System.out.println(content);

        return new Message(Role.ASSISTANT, content);
    }

    @Override
    public Message streamResponse(List<Message> messages) {
        //TODO:
        // - Build MessageCreateParams using buildParams(messages)
        // - Open a streaming call via client.messages().createStreaming(params) (try-with-resources)
        // - Filter events where isContentBlockDelta() is true
        // - From each event's delta, check isText(); extract text via asText().text()
        // - Print each non-empty text to stdout; accumulate in a StringBuilder
        // - Print a newline after the stream ends
        // - Return new Message(Role.ASSISTANT, accumulated content)
        MessageCreateParams messageCreateParams = buildParams(messages);
        try(StreamResponse<RawMessageStreamEvent> streaming = client.messages().createStreaming(messageCreateParams)) {
            StringBuilder sb = new StringBuilder();
            streaming.stream().filter(RawMessageStreamEvent::isContentBlockDelta)
                .map(s -> s.asContentBlockDelta().delta())
                .filter(RawContentBlockDelta::isText)
                .map(s -> s.asText().text())
                .filter(s -> !s.isEmpty())
                .forEach(s -> {
                    System.out.print(s);
                    sb.append(s);
                });
            System.out.println();
            return new Message(Role.ASSISTANT, sb.toString());
        } catch (RuntimeException e) {
            throw new RuntimeException("AnthropicAiClient streaming failed", e);
        }
    }

    private MessageCreateParams buildParams(List<Message> messages) {
        //TODO:
        // - Create a MessageCreateParams builder; set model, system (systemPrompt), and maxTokens (e.g. 1024)
        // - Iterate messages: USER → addUserMessage(), ASSISTANT → addAssistantMessage()
        // - Build and return the params
        MessageCreateParams.Builder builder = MessageCreateParams.builder();
        builder.model(modelName);
        builder.system(systemPrompt);
        builder.maxTokens(1024);

        messages.forEach(message -> {
            switch (message.role()) {
                case USER -> builder.addUserMessage(message.content());
                case ASSISTANT -> builder.addAssistantMessage(message.content());
                default -> throw new RuntimeException("Unknown role: " + message.role());
            }
        });

        return builder.build();
    }
}
