package t1.llm.api.openai.chat.completions;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.http.StreamResponse;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import commons.exceptions.TaskNotImplementedException;
import commons.model.Message;
import commons.model.Role;
import java.util.List;
import t1.llm.api.openai.BaseOpenAiClient;

/**
 * OpenAI Chat Completions client using the official OpenAI Java SDK.
 * <p>
 * Demonstrates how the SDK abstracts HTTP and SSE details. Compare with
 * {@link CustomOpenAiChatCompletionsClient} which does the same via raw HTTP.
 */
public class OpenAiChatCompletionsClient extends BaseOpenAiClient {

    private OpenAIClient client;

    public OpenAiChatCompletionsClient(String endpoint, String modelName, String apiKey, String systemPrompt) {
        super(endpoint, modelName, apiKey, systemPrompt);
        this.client = OpenAIOkHttpClient.builder()
            .apiKey(apiKey)
            .build();
    }

    @Override
    public Message response(List<Message> messages) {
        //TODO:
        // - Build ChatCompletionCreateParams using buildParams(messages)
        // - Call client.chat().completions().create(params)
        // - Extract content string from choices[0].message.content() (throw if absent)
        // - Print content to stdout
        // - Return new Message(Role.ASSISTANT, content)
        ChatCompletionCreateParams params = buildParams(messages);
        ChatCompletion chatCompletion = client.chat().completions().create(params);
        String content = chatCompletion.choices().getFirst().message().content().orElseThrow(() -> new RuntimeException("No content available"));

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
        ChatCompletionCreateParams params = buildParams(messages);
        StringBuilder stringBuilder = new StringBuilder();
        try(StreamResponse<ChatCompletionChunk> streaming = client.chat().completions().createStreaming(params)) {
            streaming.stream().forEach(chunk -> {
                chunk.choices().getFirst().delta().content().ifPresent(content -> {
                    System.out.print(content);
                    stringBuilder.append(content);
                });
            });
        }
        System.out.println();
        return new Message(Role.ASSISTANT, stringBuilder.toString());
    }

    private ChatCompletionCreateParams buildParams(List<Message> messages) {
        //TODO:
        // - Create a ChatCompletionCreateParams builder; set model and add the system message (systemPrompt)
        // - Iterate messages: USER → addUserMessage(), ASSISTANT → addMessage() with ChatCompletionAssistantMessageParam
        // - Build and return the params
        ChatCompletionCreateParams.Builder completionCreateParamsBuilder = ChatCompletionCreateParams.builder()
            .model(modelName)
            .addSystemMessage(systemPrompt);

        messages.forEach(message -> {
            switch (message.role()) {
                case USER -> completionCreateParamsBuilder.addUserMessage(message.content());
                case ASSISTANT -> ChatCompletionAssistantMessageParam.builder().content(message.content()).build();
                default -> throw new IllegalArgumentException("Unknown role: " + message.role());

            }
        });
        return completionCreateParamsBuilder.build();
    }
}
