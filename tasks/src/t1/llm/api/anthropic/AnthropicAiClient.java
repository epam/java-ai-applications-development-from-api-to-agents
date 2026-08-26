package t1.llm.api.anthropic;

import java.util.List;
import java.util.stream.Collectors;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.RawMessageStreamEvent;
import com.anthropic.models.messages.TextBlock;
import commons.model.Message;
import commons.model.Role;
import t1.llm.api.AiClient;

/**
 * Anthropic Claude client using the official Anthropic Java SDK.
 * <p>
 * Claude's API differs from OpenAI: the system prompt is a separate {@code system} parameter, not a message in the
 * conversation. Max tokens must always be specified. Compare with {@link CustomAnthropicAiClient} for the raw HTTP
 * equivalent.
 */
public class AnthropicAiClient extends AiClient {

    private AnthropicClient client;

    public AnthropicAiClient(String endpoint, String modelName, String apiKey, String systemPrompt) {
        super(endpoint, modelName, apiKey, systemPrompt);
        //TODO:
        // - https://github.com/anthropics/anthropic-sdk-java
        // - Build an AnthropicClient using AnthropicOkHttpClient.builder(), set apiKey, and call build()
        // - Assign the result to this.client
        this.client = AnthropicOkHttpClient.builder()
            .apiKey(apiKey)
            .build();
    }

    @Override
    public Message response(List<Message> messages) {
        //TODO:
        // - Build MessageCreateParams using buildParams(messages)
        // - Call client.messages().create(params)
        // - Filter the response content blocks for isText(); extract text via asText().text(); concatenate
        // - Print content to stdout
        // - Return new Message(Role.ASSISTANT, content)

        var params = buildParams(messages);
        var response = client.messages().create(params);
        var message = response.content()
            .stream()
            .filter(ContentBlock::isText)
            .map(ContentBlock::asText)
            .map(TextBlock::text)
            .collect(Collectors.joining(""));
        System.out.println(message);
        return new Message(Role.ASSISTANT, message);
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
        var params = buildParams(messages);
        var sb = new StringBuilder();
        try (var stream = client.messages().createStreaming(params)) {
            stream.stream()
                .filter(RawMessageStreamEvent::isContentBlockDelta)
                .forEach(event -> {
                    var delta = event.asContentBlockDelta().delta();
                    if (delta.isText()) {
                        var content = delta.asText().text();
                        if (!content.isEmpty()) {
                            System.out.print(content);
                            sb.append(content);
                        }
                    }
                });
        }
        System.out.println();
        return new Message(Role.ASSISTANT, sb.toString());
    }

    private MessageCreateParams buildParams(List<Message> messages) {
        //TODO:
        // - Create a MessageCreateParams builder; set model, system (systemPrompt), and maxTokens (e.g. 1024)
        // - Iterate messages: USER → addUserMessage(), ASSISTANT → addAssistantMessage()
        // - Build and return the params
        var builder = MessageCreateParams.builder()
            .model(modelName)
            .system(systemPrompt)
            .maxTokens(1024);

        for (Message message : messages) {
            switch (message.role()) {
                case ASSISTANT -> builder.addAssistantMessage(message.content());
                case USER -> builder.addUserMessage(message.content());
            }
        }
        return builder.build();
    }
}
