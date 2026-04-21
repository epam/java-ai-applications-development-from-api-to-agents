package t1.llm.api.gemini;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.Part;
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

    private final Client client;

    public GeminiAiClient(String endpoint, String modelName,
                          String apiKey, String systemPrompt) {
        super(endpoint, modelName, apiKey, systemPrompt);
        this.client = Client.builder()
                .apiKey(apiKey)
                .build();
    }

    @Override
    public Message response(List<Message> messages) {
        var config = buildConfig();
        var contents = buildContents(messages);
        var resp = client.models.generateContent(modelName, contents, config);
        String content = resp.text() != null ? resp.text() : "";
        System.out.println(content);
        return new Message(Role.ASSISTANT, content);
    }

    @Override
    public Message streamResponse(List<Message> messages) {
        var config = buildConfig();
        var contents = buildContents(messages);
        var sb = new StringBuilder();
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
        return GenerateContentConfig.builder()
                .systemInstruction(Content.builder()
                        .parts(List.of(Part.fromText(systemPrompt)))
                        .build())
                .maxOutputTokens(1024)
                .build();
    }

    private List<Content> buildContents(List<Message> messages) {
        var contents = new ArrayList<Content>();
        for (Message m : messages) {
            contents.add(Content.builder()
                    .role(toGeminiRole(m.role()))
                    .parts(List.of(Part.fromText(m.content())))
                    .build());
        }
        return contents;
    }

    private String toGeminiRole(Role role) {
        return role == Role.ASSISTANT ? "model" : role.getValue();
    }
}