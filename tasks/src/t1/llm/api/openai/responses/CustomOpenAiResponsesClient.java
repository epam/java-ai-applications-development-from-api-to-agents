package t1.llm.api.openai.responses;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import commons.model.Message;
import commons.model.Role;
import t1.llm.api.openai.BaseOpenAiClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * OpenAI Responses API client using raw HTTP — no SDK.
 * <p>
 * The Responses API uses an event-based SSE format different from Chat Completions:
 * each SSE frame consists of an "event: &lt;type&gt;" line followed by "data: &lt;json&gt;".
 * Only frames with type "response.output_text.delta" carry text content.
 */
public class CustomOpenAiResponsesClient extends BaseOpenAiClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();

    public CustomOpenAiResponsesClient(String endpoint, String modelName,
                                        String apiKey, String systemPrompt) {
        super(endpoint, modelName, apiKey, systemPrompt);
    }

    @Override
    public Message response(List<Message> messages) {
        try {
            String body = buildRequestBody(messages, false);
            HttpRequest request = buildRequest(body);
            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new RuntimeException("HTTP " + resp.statusCode() + ": " + resp.body());
            }
            String content = extractOutputText(MAPPER.readTree(resp.body()));
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
            String body = buildRequestBody(messages, true);
            HttpRequest request = buildRequest(body);
            HttpResponse<Stream<String>> resp =
                    http.send(request, HttpResponse.BodyHandlers.ofLines());
            var sb = new StringBuilder();
            String[] currentEvent = {null};
            Iterator<String> iter = resp.body().iterator();
            while (iter.hasNext()) {
                String line = iter.next();
                if (line.startsWith("event: ")) {
                    currentEvent[0] = line.substring(7).strip();
                } else if (line.startsWith("data: ")
                        && "response.output_text.delta".equals(currentEvent[0])) {
                    JsonNode data = MAPPER.readTree(line.substring(6));
                    String delta = data.path("delta").asText("");
                    if (!delta.isEmpty()) {
                        System.out.print(delta);
                        sb.append(delta);
                    }
                } else if (line.isEmpty()) {
                    currentEvent[0] = null;
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

    private HttpRequest buildRequest(String body) {
        return HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Authorization", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private String buildRequestBody(List<Message> messages, boolean stream) {
        try {
            var inputMessages = messages.stream()
                    .map(Message::toMap)
                    .toList();
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", modelName);
            body.put("instructions", systemPrompt);
            body.put("input", inputMessages);
            if (stream) {
                body.put("stream", true);
            }
            return MAPPER.writeValueAsString(body);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String extractOutputText(JsonNode root) {
        for (JsonNode item : root.path("output")) {
            if ("message".equals(item.path("type").asText())) {
                for (JsonNode contentPart : item.path("content")) {
                    if ("output_text".equals(contentPart.path("type").asText())) {
                        return contentPart.path("text").asText("");
                    }
                }
            }
        }
        throw new RuntimeException("No output text found in the response");
    }
}
