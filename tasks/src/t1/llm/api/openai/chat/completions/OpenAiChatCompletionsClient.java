package t1.llm.api.openai.chat.completions;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import commons.exceptions.TaskNotImplementedException;
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

    private OpenAIClient client;

    public OpenAiChatCompletionsClient(String endpoint, String modelName, String apiKey, String systemPrompt) {
        super(endpoint, modelName, apiKey, systemPrompt);
        // TODO:
        // https://github.com/openai/openai-java
        // 1. Build SDK client and assign to this.client:
        //    this.client = OpenAIOkHttpClient.builder().apiKey(apiKey).build()
    }

    @Override
    public Message response(List<Message> messages) {
        // TODO:
        // 1. Build params: var params = buildParams(messages)
        // 2. Call client.chat().completions().create(params) to get the completion
        // 3. Extract content from choices[0].message.content() — throw RuntimeException if absent
        // 4. Print content and return new Message(Role.ASSISTANT, content)
        throw new TaskNotImplementedException();
    }

    @Override
    public Message streamResponse(List<Message> messages) {
        // TODO:
        // 1. Build params and create a StringBuilder to accumulate tokens
        // 2. Open streaming via client.chat().completions().createStreaming(params) in try-with-resources
        // 3. For each chunk in stream.stream(), extract the delta token:
        //    chunk.choices().get(0).delta().content().ifPresent(delta -> { print delta; append to sb })
        // 4. Print newline and return new Message(Role.ASSISTANT, sb.toString())
        throw new TaskNotImplementedException();
    }

    private ChatCompletionCreateParams buildParams(List<Message> messages) {
        // TODO:
        // 1. Create builder with model and system message:
        //    ChatCompletionCreateParams.builder().model(modelName).addSystemMessage(systemPrompt)
        // 2. For each message add it by role:
        //    - USER → builder.addUserMessage(m.content())
        //    - ASSISTANT → builder.addMessage(ChatCompletionAssistantMessageParam.builder().content(m.content()).build())
        // 3. Return builder.build()
        throw new TaskNotImplementedException();
    }
}