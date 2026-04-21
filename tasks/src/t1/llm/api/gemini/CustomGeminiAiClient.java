package t1.llm.api.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import t1.llm.api.AiClient;
import commons.model.Message;
import commons.model.Role;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Google Gemini client using raw HTTP — no official stable Java SDK available.
 * <p>
 * Key differences from OpenAI/Anthropic:
 * <ul>
 *   <li>Auth header is {@code x-goog-api-key} (not Authorization/x-api-key)</li>
 *   <li>System prompt goes in {@code system_instruction.parts[].text}</li>
 *   <li>The role for AI messages is {@code "model"}, not {@code "assistant"}</li>
 *   <li>Non-streaming URL: {@code {endpoint}/{model}:generateContent}</li>
 *   <li>Streaming URL: {@code {endpoint}/{model}:streamGenerateContent?alt=sse}</li>
 *   <li>Response path: {@code candidates[0].content.parts[*].text}</li>
 * </ul>
 */
public class CustomGeminiAiClient extends AiClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();

    public CustomGeminiAiClient(String endpoint, String modelName,
                                 String apiKey, String systemPrompt) {
        super(endpoint, modelName, apiKey, systemPrompt);
    }

    @Override
    public Message response(List<Message> messages) {
        try {
            String url = endpoint + "/" + modelName + ":generateContent";
            String body = buildRequestBody(messages);
            HttpRequest request = buildRequest(url, body);
            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new RuntimeException("HTTP " + resp.statusCode() + ": " + resp.body());
            }
            String content = extractPartsText(MAPPER.readTree(resp.body()).path("candidates").get(0));
            System.out.println(content);
            return new Message(Role.ASSISTANT, content);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Message streamResponse(List<Message> messages) {
        try {
            String url = endpoint + "/" + modelName + ":streamGenerateContent?alt=sse";
            String body = buildRequestBody(messages);
            HttpRequest request = buildRequest(url, body);
            HttpResponse<Stream<String>> resp =
                    http.send(request, HttpResponse.BodyHandlers.ofLines());
            var sb = new StringBuilder();
            Iterator<String> iter = resp.body().iterator();
            while (iter.hasNext()) {
                String line = iter.next();
                if (!line.startsWith("data: ")) {
                    continue;
                }
                JsonNode parsed = MAPPER.readTree(line.substring(6).strip());
                JsonNode candidates = parsed.path("candidates");
                if (candidates.isArray() && !candidates.isEmpty()) {
                    String text = extractPartsText(candidates.get(0));
                    if (!text.isEmpty()) {
                        System.out.print(text);
                        sb.append(text);
                    }
                }
            }
            System.out.println();
            return new Message(Role.ASSISTANT, sb.toString());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private HttpRequest buildRequest(String url, String body) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private String buildRequestBody(List<Message> messages) {
        try {
            List<Map<String, Object>> contents = new ArrayList<>();
            for (Message m : messages) {
                contents.add(Map.of(
                        "role", toGeminiRole(m.role()),
                        "parts", List.of(Map.of("text", m.content()))
                ));
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("system_instruction", Map.of("parts", List.of(Map.of("text", systemPrompt))));
            body.put("contents", contents);
            body.put("generationConfig", Map.of("maxOutputTokens", 1024));
            return MAPPER.writeValueAsString(body);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String extractPartsText(JsonNode candidate) {
        var sb = new StringBuilder();
        for (JsonNode part : candidate.path("content").path("parts")) {
            String text = part.path("text").asText("");
            sb.append(text);
        }
        return sb.toString();
    }

    /** Gemini uses "model" for AI responses instead of "assistant". */
    private String toGeminiRole(Role role) {
        return role == Role.ASSISTANT ? "model" : role.getValue();
    }
}
