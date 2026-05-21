package t1.llm.api.openai.chat.completions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import commons.model.Message;
import commons.model.Role;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import t1.llm.api.openai.BaseOpenAiClient;

/**
 * OpenAI Chat Completions client using raw HTTP — no SDK.
 * <p>
 * Shows what the SDK does under the hood: plain REST POST with JSON body,
 * and SSE line-by-line parsing for streaming.
 * The "data: [DONE]" sentinel marks the end of the stream.
 */
public class CustomOpenAiChatCompletionsClient extends BaseOpenAiClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();

    public CustomOpenAiChatCompletionsClient(String endpoint, String modelName, String apiKey, String systemPrompt) {
        super(endpoint, modelName, apiKey, systemPrompt);
    }

    @Override
    public Message response(List<Message> messages) {
        //TODO:
        // https://platform.openai.com/docs/api-reference/chat/create
        // - Build JSON body using buildRequestBody(messages, false)
        // - Build HttpRequest using buildRequest(body)
        // - Send with HttpClient using BodyHandlers.ofString()
        // - Throw RuntimeException if response status is not 200
        // - Parse JSON with ObjectMapper; extract content at /choices/0/message/content
        // - Print content to stdout
        // - Return new Message(Role.ASSISTANT, content)
        // - Wrap all checked exceptions in RuntimeException
        try {
            String requestBody = buildRequestBody(messages, false);
            HttpRequest httpRequest = buildRequest(requestBody);
            HttpResponse<String> httpResponse = http.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + httpResponse.statusCode());
            }

            JsonNode root = MAPPER.readTree(httpResponse.body());   // parse JSON string → tree
            String content = root.at("/choices/0/message/content").asText();
            System.out.println(content);

            return new Message(Role.ASSISTANT, content);

        } catch (Exception e) {
            throw new RuntimeException("API call failed", e);
        }
    }

    @Override
    public Message streamResponse(List<Message> messages) {
        //TODO:
        // https://platform.openai.com/docs/api-reference/chat/create (Streaming tab)
        // - Build JSON body using buildRequestBody(messages, true)
        // - Build HttpRequest using buildRequest(body)
        // - Send with HttpClient using BodyHandlers.ofLines() to get a Stream<String>
        // - Filter lines starting with "data: "; strip the prefix
        // - Stop processing when the sentinel "[DONE]" is encountered (use takeWhile)
        // - For each remaining JSON line, parse with ObjectMapper; extract /choices/0/delta/content
        // - Print each non-empty delta to stdout; accumulate in a StringBuilder
        // - Print a newline after the stream ends
        // - Return new Message(Role.ASSISTANT, accumulated content)
        // - Wrap all checked exceptions in RuntimeException
        try {
            String requestBody = buildRequestBody(messages, true);
            HttpRequest httpRequest = buildRequest(requestBody);
            HttpResponse<Stream<String>> httpResponse = http.send(httpRequest, HttpResponse.BodyHandlers.ofLines());

            if (httpResponse.statusCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + httpResponse.statusCode());
            }

            StringBuilder fullMessage = new StringBuilder();
            httpResponse.body()
                .filter(m -> m.startsWith("data: "))
                .map(m -> m.substring(6).strip())
                .takeWhile(m -> !"[DONE]".equals(m))
                .forEach(m -> {
                    JsonNode root = null;   // parse JSON string → tree
                    try {
                        root = MAPPER.readTree(m);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("JSON parsing failed", e);
                    }
                    String content = root.at("/choices/0/delta/content").asText();
                    System.out.print(content);
                    fullMessage.append(content);
                });

            System.out.println();
            return new Message(Role.ASSISTANT, fullMessage.toString());
        } catch (Exception e) {
            throw new RuntimeException("API call failed", e);
        }
    }

    private HttpRequest buildRequest(String body) {
        //TODO:
        // - Build an HttpRequest.Builder with URI from endpoint
        // - Add "Authorization" header using apiKey (already contains "Bearer " prefix)
        // - Add "Content-Type: application/json" header
        // - Set POST body with HttpRequest.BodyPublishers.ofString(body)
        // - Build and return the HttpRequest
        HttpRequest.Builder builder = HttpRequest.newBuilder();
        builder.uri(URI.create(this.endpoint));
        builder.header("Authorization", this.apiKey);
        builder.header("Content-Type", "application/json");
        builder.POST(HttpRequest.BodyPublishers.ofString(body));
        return builder.build();
    }

    private String buildRequestBody(List<Message> messages, boolean stream) {
        //TODO:
        // - Create a messages list; prepend the system message as Map("role"->"system", "content"->systemPrompt)
        // - Convert each Message to a map via Message.toMap() and append
        // - Build a body LinkedHashMap with "model" and "messages" keys
        // - If stream is true, add "stream": true
        // - Serialize to JSON string with ObjectMapper and return
        // - Wrap checked exceptions in RuntimeException

        try {
            List<Map<String, Object>> processedMessages = new ArrayList<>();
            processedMessages.add(Map.of("role", Role.SYSTEM.getValue(), "content", systemPrompt));

            messages.forEach(message -> processedMessages.add(message.toMap()));
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("model", modelName);
            map.put("messages", processedMessages);

            if (stream) {
                map.put("stream", stream);
            }

            return MAPPER.writeValueAsString(map);

        } catch (Exception e) {
            throw new RuntimeException("Error building request body", e);
        }
    }
}
