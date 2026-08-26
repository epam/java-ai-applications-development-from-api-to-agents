package t1.llm.api.openai.chat.completions;

import java.util.List;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import commons.model.Message;
import commons.model.Role;
import t1.llm.api.openai.BaseOpenAiClient;

/**
 * OpenAI Chat Completions client using the official OpenAI Java SDK.
 * <p>
 * Demonstrates how the SDK abstracts HTTP and SSE details. Compare with {@link CustomOpenAiChatCompletionsClient} which
 * does the same via raw HTTP.
 */
public class OpenAiChatCompletionsClient extends BaseOpenAiClient {

    private OpenAIClient client;

    public OpenAiChatCompletionsClient(String endpoint, String modelName, String apiKey, String systemPrompt) {
        super(endpoint, modelName, apiKey, systemPrompt);
        this.client = OpenAIOkHttpClient.builder().apiKey(apiKey).build();
        //TODO:
        // https://github.com/openai/openai-java
        // - Call super(endpoint, modelName, apiKey, systemPrompt)
        // - Build an OpenAIClient using OpenAIOkHttpClient.builder(), set apiKey, and call build()
        // - Assign the result to this.client
    }

    @Override
    public Message response(List<Message> messages) {
        //TODO:
        // - Build ChatCompletionCreateParams using buildParams(messages)
        // - Call client.chat().completions().create(params)
        // - Extract content string from choices[0].message.content() (throw if absent)
        // - Print content to stdout
        // - Return new Message(Role.ASSISTANT, content)
        var params = buildParams(messages);
        var response = client.chat().completions().create(params);
        var content = response.choices().getFirst().message().content().orElseThrow();
        System.out.println(content);
        return new Message(Role.ASSISTANT, content);
    }

    @Override
    public Message streamResponse(List<Message> messages) {
        //TODO:
        // - Build ChatCompletionCreateParams using buildParams(messages)
        // - Open a streaming call via client.chat().completions().createStreaming(params) (try-with-resources)
        // - For each chunk, extract delta content from choices[0].delta.content()
        // - Print each non-empty delta token to stdout; accumulate in a StringBuilder
        // - Print a newline after the stream ends
        // - Return new Message(Role.ASSISTANT, accumulated content)
        var params = buildParams(messages);
        var sb = new StringBuilder();
        try (var streamingContent = client.chat().completions().createStreaming(params)) {
            streamingContent.stream()
                .forEach(chunk -> {
                    chunk.choices().getFirst().delta().content().ifPresent(delta -> {
                        System.out.print(delta);
                        sb.append(delta);
                    });
                });
        }
        System.out.println();
        return new Message(Role.ASSISTANT, sb.toString());
    }

    private ChatCompletionCreateParams buildParams(List<Message> messages) {
        //TODO:
        // - Create a ChatCompletionCreateParams builder; set model and add the system message (systemPrompt)
        // - Iterate messages: USER → addUserMessage(), ASSISTANT → addMessage() with ChatCompletionAssistantMessageParam
        // - Build and return the params

        var builder = ChatCompletionCreateParams.builder()
            .model(modelName)
            .addSystemMessage(systemPrompt);

        messages.forEach(message -> {
            if (message.role().equals(Role.USER)) {
                builder.addUserMessage(message.content());
            } else if (message.role().equals(Role.ASSISTANT)) {
                builder.addMessage(ChatCompletionAssistantMessageParam.builder()
                    .content(message.content())
                    .build());
            }
        });
        return builder.build();
    }
}
