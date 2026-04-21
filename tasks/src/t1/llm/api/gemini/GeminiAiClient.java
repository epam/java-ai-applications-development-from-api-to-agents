package t1.llm.api.gemini;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.Part;
import commons.exceptions.TaskNotImplementedException;
import commons.model.Message;
import commons.model.Role;
import t1.llm.api.AiClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Google Gemini client using the official Google GenAI Java SDK.
 * <p>
 * Key differences from OpenAI/Anthropic:
 * <ul>
 *   <li>System prompt goes in {@code GenerateContentConfig.systemInstruction}</li>
 *   <li>The role for AI messages is {@code "model"}, not {@code "assistant"}</li>
 *   <li>Streaming uses {@code ResponseStream<GenerateContentResponse>} which is {@code Iterable}</li>
 * </ul>
 * Compare with {@link CustomGeminiAiClient} for the raw HTTP equivalent.
 */
public class GeminiAiClient extends AiClient {

    private Client client;

    public GeminiAiClient(String endpoint, String modelName, String apiKey, String systemPrompt) {
        super(endpoint, modelName, apiKey, systemPrompt);
        // TODO:
        // https://github.com/googleapis/java-genai
        // 1. Build the Google GenAI client and assign it to this.client:
        //    this.client = Client.builder().apiKey(apiKey).build()
    }

    @Override
    public Message response(List<Message> messages) {
        // TODO:
        // 1. Build config using buildConfig() and contents list using buildContents(messages)
        // 2. Call client.models.generateContent(modelName, contents, config)
        // 3. Extract text: resp.text() can return null — treat null as empty string
        // 4. Print content and return new Message(Role.ASSISTANT, content)
        throw new TaskNotImplementedException();
    }

    @Override
    public Message streamResponse(List<Message> messages) {
        // TODO:
        // 1. Build config using buildConfig() and contents list using buildContents(messages)
        // 2. Open streaming with try-with-resources (ResponseStream is AutoCloseable and Iterable):
        //    try (var stream = client.models.generateContentStream(modelName, contents, config))
        // 3. Iterate over each chunk; chunk.text() can be null — skip null/empty values;
        //    print and accumulate non-empty text in a StringBuilder
        // 4. Print newline and return new Message(Role.ASSISTANT, sb.toString())
        throw new TaskNotImplementedException();
    }

    private GenerateContentConfig buildConfig() {
        // TODO:
        // Note: In Gemini the system prompt goes in the config's systemInstruction field, not in messages!
        // 1. Build and return config:
        //    return GenerateContentConfig.builder()
        //            .systemInstruction(Content.builder()
        //                    .parts(List.of(Part.fromText(systemPrompt)))
        //                    .build())
        //            .maxOutputTokens(1024)
        //            .build()
        throw new TaskNotImplementedException();
    }

    private List<Content> buildContents(List<Message> messages) {
        // TODO:
        // Note: Gemini uses "model" for AI responses, not "assistant"
        // 1. Create result list: var contents = new ArrayList<Content>()
        // 2. For each message build a Content and add it:
        //    contents.add(Content.builder()
        //            .role(toGeminiRole(m.role()))
        //            .parts(List.of(Part.fromText(m.content())))
        //            .build())
        // 3. Return the list
        throw new TaskNotImplementedException();
    }

    private String toGeminiRole(Role role) {
        // TODO:
        // Gemini uses "model" for AI responses instead of "assistant"
        // 1. Return "model" if role == Role.ASSISTANT, otherwise return role.getValue()
        throw new TaskNotImplementedException();
    }
}