package t1.llm.api.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import commons.exceptions.TaskNotImplementedException;
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

    public CustomGeminiAiClient(String endpoint, String modelName, String apiKey, String systemPrompt) {
        super(endpoint, modelName, apiKey, systemPrompt);
    }

    @Override
    public Message response(List<Message> messages) {
        //TODO:
        // https://ai.google.dev/api/generate-content
        // - Build the non-streaming URL: endpoint + "/" + modelName + ":generateContent"
        // - Build JSON body using buildRequestBody(messages)
        // - Build HttpRequest using buildRequest(url, body)
        // - Send with HttpClient using BodyHandlers.ofString()
        // - Throw RuntimeException if response status is not 200
        // - Parse JSON with ObjectMapper; access candidates[0]; extract text using extractPartsText()
        // - Print content to stdout
        // - Return new Message(Role.ASSISTANT, content)
        // - Wrap all checked exceptions in RuntimeException
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
        //TODO:
        // https://ai.google.dev/api/generate-content#method:-models.streamgeneratecontent
        // - Build the streaming URL: endpoint + "/" + modelName + ":streamGenerateContent?alt=sse"
        // - Build JSON body using buildRequestBody(messages)
        // - Build HttpRequest using buildRequest(url, body)
        // - Send with HttpClient using BodyHandlers.ofLines()
        // - Iterate lines starting with "data: "; parse JSON from each
        // - Access the "candidates" array; extract text from candidates[0] using extractPartsText()
        // - Print each non-empty text to stdout; accumulate in a StringBuilder
        // - Print a newline after the stream ends
        // - Return new Message(Role.ASSISTANT, accumulated content)
        // - Wrap all checked exceptions in RuntimeException
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
        //TODO:
        // - Build an HttpRequest.Builder with URI from the given url string
        // - Add "Content-Type: application/json" header
        // - Add "x-goog-api-key" header with apiKey (Gemini uses this instead of Authorization)
        // - Set POST body with HttpRequest.BodyPublishers.ofString(body)
        // - Build and return the HttpRequest
        return HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .header("x-goog-api-key", apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    }

    private String buildRequestBody(List<Message> messages) {
        //TODO:
        // - Build "system_instruction" as a Map containing "parts": list of {"text": systemPrompt}
        // - Build "contents" list: for each Message, create a Map with
        //     "role": toGeminiRole(m.role()) and "parts": list of {"text": m.content()}
        // - Build a body LinkedHashMap with "system_instruction", "contents",
        //   and "generationConfig" containing "maxOutputTokens"
        // - Serialize to JSON string with ObjectMapper and return
        // - Wrap checked exceptions in RuntimeException
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
        //TODO:
        // - Iterate over the candidate's content.parts array
        // - For each part, extract the "text" field value and append to a StringBuilder
        // - Return the concatenated string
        var sb = new StringBuilder();
        for (JsonNode part : candidate.path("content").path("parts")) {
            String text = part.path("text").asText("");
            sb.append(text);
        }
        return sb.toString();
    }

    private String toGeminiRole(Role role) {
        //TODO:
        // - Return "model" if the role is Role.ASSISTANT (Gemini uses "model" not "assistant")
        // - Otherwise return role.getValue()
        return role == Role.ASSISTANT ? "model" : role.getValue();
    }
}
