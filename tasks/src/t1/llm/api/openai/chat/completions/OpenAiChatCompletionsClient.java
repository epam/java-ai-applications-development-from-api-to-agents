package t1.llm.api.openai.chat.completions;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import commons.model.Message;
import commons.model.Role;
import t1.llm.api.openai.BaseOpenAiClient;

import java.util.List;

/**
 * OpenAI Chat Completions client using the official OpenAI Java SDK.
 * <p>
 * Demonstrates how the SDK abstracts HTTP and SSE details. Compare with
 * {@link CustomOpenAiChatCompletionsClient} which does the same via raw HTTP.
 */
public class OpenAiChatCompletionsClient extends BaseOpenAiClient {

    private final OpenAIClient client;

    public OpenAiChatCompletionsClient(String endpoint, String modelName,
                                       String apiKey, String systemPrompt) {
        super(endpoint, modelName, apiKey, systemPrompt);
        this.client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }

    @Override
    public Message response(List<Message> messages) {
        var params = buildParams(messages);
        var completion = client.chat().completions().create(params);
        String content = completion.choices().get(0).message().content()
                .orElseThrow(() -> new RuntimeException("No content in response"));
        System.out.println(content);
        return new Message(Role.ASSISTANT, content);
    }

    @Override
    public Message streamResponse(List<Message> messages) {
        var params = buildParams(messages);
        var sb = new StringBuilder();
        try (var stream = client.chat().completions().createStreaming(params)) {
            stream.stream().forEach(chunk ->
                    chunk.choices().get(0).delta().content().ifPresent(delta -> {
                        System.out.print(delta);
                        sb.append(delta);
                    })
            );
        }
        System.out.println();
        return new Message(Role.ASSISTANT, sb.toString());
    }

    private ChatCompletionCreateParams buildParams(List<Message> messages) {
        var builder = ChatCompletionCreateParams.builder()
                .model(modelName)
                .addSystemMessage(systemPrompt);
        for (Message m : messages) {
            switch (m.role()) {
                case USER -> builder.addUserMessage(m.content());
                case ASSISTANT -> builder.addMessage(
                        ChatCompletionAssistantMessageParam.builder().content(m.content()).build()
                );
                default -> { /* system messages are not expected in conversation history */ }
            }
        }
        return builder.build();
    }
}
