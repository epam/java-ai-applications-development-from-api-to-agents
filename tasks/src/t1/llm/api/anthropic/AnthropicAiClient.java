package t1.llm.api.anthropic;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.MessageCreateParams;
import commons.exceptions.TaskNotImplementedException;
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

    private AnthropicClient client;

    public AnthropicAiClient(String endpoint, String modelName, String apiKey, String systemPrompt) {
        super(endpoint, modelName, apiKey, systemPrompt);
        // TODO:
        // https://github.com/anthropics/anthropic-sdk-java
        // 1. Build the Anthropic SDK client and assign it to this.client:
        //    this.client = AnthropicOkHttpClient.builder().apiKey(apiKey).build()
    }

    @Override
    public Message response(List<Message> messages) {
        // TODO:
        // 1. Build params using buildParams(messages) and call client.messages().create(params)
        // 2. Filter the response content blocks using isText() and extract text via asText().text();
        //    concatenate all text blocks into a single string
        // 3. Print content and return new Message(Role.ASSISTANT, content)
        throw new TaskNotImplementedException();
    }

    @Override
    public Message streamResponse(List<Message> messages) {
        // TODO:
        // 1. Build params and open streaming via client.messages().createStreaming(params) in try-with-resources
        // 2. Filter events with: stream.stream().filter(RawMessageStreamEvent::isContentBlockDelta)
        // 3. For each event, get the delta via event.asContentBlockDelta().delta()
        //    If delta.isText() is true, extract text with delta.asText().text(); print and accumulate in StringBuilder
        // 4. Print newline and return new Message(Role.ASSISTANT, sb.toString())
        throw new TaskNotImplementedException();
    }

    private MessageCreateParams buildParams(List<Message> messages) {
        // TODO:
        // Note: Anthropic requires maxTokens and puts the system prompt as a separate field!
        // 1. Create builder with required fields:
        //    var builder = MessageCreateParams.builder()
        //            .model(modelName)
        //            .system(systemPrompt)    // separate field, NOT in the messages array
        //            .maxTokens(1024L)        // required — Anthropic will reject requests without it
        // 2. Iterate messages and add each by role:
        //    - Role.USER → builder.addUserMessage(m.content())
        //    - Role.ASSISTANT → builder.addAssistantMessage(m.content())
        //    (skip other roles)
        // 3. Return: return builder.build()
        throw new TaskNotImplementedException();
    }
}