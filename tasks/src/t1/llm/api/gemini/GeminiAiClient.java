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
        //TODO:
        // https://github.com/googleapis/java-genai
        // - Build a Client using Client.builder(), set apiKey, and call build()
        // - Assign the result to this.client
        this.client = Client.builder().apiKey(apiKey).build();
    }

    @Override
    public Message response(List<Message> messages) {
        //TODO:
        // - Build GenerateContentConfig using buildConfig()
        // - Build the contents list using buildContents(messages)
        // - Call client.models.generateContent(modelName, contents, config)
        // - Extract text from response via resp.text(); treat null as empty string
        // - Print content to stdout
        // - Return new Message(Role.ASSISTANT, content)
        GenerateContentConfig config = buildConfig();
        List<Content> contents = buildContents(messages);
        var resp = client.models.generateContent(modelName, contents, config);
        String content = resp.text() != null ? resp.text() : "";
        System.out.println(content);
        return new Message(Role.ASSISTANT, content);
    }

    @Override
    public Message streamResponse(List<Message> messages) {
        //TODO:
        // - Build GenerateContentConfig using buildConfig()
        // - Build the contents list using buildContents(messages)
        // - Open a streaming call via client.models.generateContentStream(modelName, contents, config) (try-with-resources)
        // - For each chunk, extract chunk.text(); skip null or empty values
        // - Print each non-empty text to stdout; accumulate in a StringBuilder
        // - Print a newline after the stream ends
        // - Return new Message(Role.ASSISTANT, accumulated content)
        GenerateContentConfig config = buildConfig();
        List<Content> contents = buildContents(messages);
        StringBuilder sb = new StringBuilder();
        try (var stream = client.models.generateContentStream(modelName, contents, config)) {
            for (var chunk : stream) {
                String text = chunk.text();
                if (text != null && !text.isEmpty()) {
                    System.out.print(text);
                    sb.append(text);
                }
            }
        }
        System.out.println();
        return new Message(Role.ASSISTANT, sb.toString());
    }

    private GenerateContentConfig buildConfig() {
        //TODO:
        // - Build a GenerateContentConfig with systemInstruction set to a Content containing Part.fromText(systemPrompt)
        // - Set maxOutputTokens
        // - Build and return the config
        return GenerateContentConfig.builder()
            .systemInstruction(Content.fromParts(Part.fromText(systemPrompt)))
            .maxOutputTokens(1024)
            .build();
    }

    private List<Content> buildContents(List<Message> messages) {
        //TODO:
        // - For each Message, build a Content with:
        //   - role: toGeminiRole(m.role())
        //   - parts: a single Part.fromText(m.content())
        // - Collect all Content objects into a List and return
        List<Content> contents = new ArrayList<>();
        for (Message m : messages) {
            contents.add(Content.builder()
                .role(toGeminiRole(m.role()))
                .parts(List.of(Part.fromText(m.content())))
                .build());
        }
        return contents;
    }

    private String toGeminiRole(Role role) {
        //TODO:
        // - Return "model" if the role is Role.ASSISTANT (Gemini uses "model" not "assistant")
        // - Otherwise return role.getValue()
        return role == Role.ASSISTANT ? "model" : role.getValue();
    }
}
