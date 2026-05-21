package t1.llm.api.anthropic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import commons.exceptions.TaskNotImplementedException;
import commons.model.Message;
import commons.model.Role;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import t1.llm.api.AiClient;

/**
 * Anthropic Claude client using raw HTTP — no SDK.
 * <p>
 * Key differences from OpenAI raw HTTP:
 * <ul>
 *   <li>Auth header is {@code x-api-key} (no "Bearer" prefix)</li>
 *   <li>Requires {@code anthropic-version: 2023-06-01} header</li>
 *   <li>System prompt is a top-level JSON field, not a message in the array</li>
 *   <li>{@code max_tokens} is required</li>
 *   <li>Streaming SSE: look for {@code content_block_delta} events with {@code text_delta} type;
 *       stop early on {@code message_stop}</li>
 * </ul>
 * This class extends {@link AiClient} directly — the API key is stored raw (no "Bearer" prefix).
 */
public class CustomAnthropicAiClient extends AiClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();

    public CustomAnthropicAiClient(String endpoint, String modelName, String apiKey, String systemPrompt) {
        super(endpoint, modelName, apiKey, systemPrompt);
    }

    @Override
    public Message response(List<Message> messages) {
        //TODO:
        // https://docs.anthropic.com/en/api/messages
        // - Build JSON body using buildRequestBody(messages, false)
        // - Build HttpRequest using buildRequest(body)
        // - Send with HttpClient using BodyHandlers.ofString()
        // - Throw RuntimeException if response status is not 200
        // - Parse JSON with ObjectMapper; iterate the "content" array; collect text from blocks where type == "text"
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
            StringBuilder stringBuilder = new StringBuilder();
            JsonNode root = MAPPER.readTree(httpResponse.body());
            for (JsonNode block : root.path("content")) {
                if ("text".equals(block.path("type").asText())) {
                    stringBuilder.append(block.path("text").asText(""));
                }
            }
            System.out.println(stringBuilder);
            return new Message(Role.ASSISTANT, stringBuilder.toString());
        }catch (Exception e) {
            throw new RuntimeException("AnthropicAiClient API call failed", e);
        }
    }

    @Override
    public Message streamResponse(List<Message> messages) {
        //TODO:
        // https://docs.anthropic.com/en/api/messages-streaming
        // - Build JSON body using buildRequestBody(messages, true)
        // - Build HttpRequest using buildRequest(body)
        // - Send with HttpClient using BodyHandlers.ofLines()
        // - Iterate lines starting with "data: "; parse JSON from each
        // - For events with type "content_block_delta" where delta.type == "text_delta": extract delta.text
        // - Print each non-empty text to stdout; accumulate in a StringBuilder
        // - Stop the loop early when event type is "message_stop"
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
                .takeWhile(m -> {
                    try {
                        return !"message_stop".equals(MAPPER.readTree(m).path("type").asText());
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("JSON parsing failed", e);
                    }
                })
                .forEach(m -> {
                    try {
                        JsonNode root = MAPPER.readTree(m);
                        if ("content_block_delta".equals(root.path("type").asText())) {
                            JsonNode delta = root.path("delta");
                            if ("text_delta".equals(delta.path("type").asText())) {
                                String text = delta.path("text").asText("");
                                if (!text.isEmpty()) {
                                    System.out.print(text);
                                    fullMessage.append(text);
                                }
                            }
                        }
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("JSON parsing failed", e);
                    }
                });
            System.out.println();
            return new Message(Role.ASSISTANT, fullMessage.toString());
        } catch (Exception e) {
            throw new RuntimeException("AnthropicAiClient API call failed", e);
        }
    }

    private HttpRequest buildRequest(String body) {
        //TODO:
        // - Build an HttpRequest.Builder with URI from endpoint
        // - Add "x-api-key" header with apiKey (Anthropic does NOT use "Bearer " prefix)
        // - Add "Content-Type: application/json" header
        // - Add "anthropic-version: 2023-06-01" header (required by Anthropic API)
        // - Set POST body with HttpRequest.BodyPublishers.ofString(body)
        // - Build and return the HttpRequest
        HttpRequest.Builder builder = HttpRequest.newBuilder();
        builder.uri(URI.create(this.endpoint));
        builder.header("x-api-key", this.apiKey);
        builder.header("Content-Type", "application/json");
        builder.header("anthropic-version", "2023-06-01");
        builder.POST(HttpRequest.BodyPublishers.ofString(body));

        return builder.build();
    }

    private String buildRequestBody(List<Message> messages, boolean stream) {
        //TODO:
        // - Build a body LinkedHashMap with "model", "system" (systemPrompt), "max_tokens" (e.g. 1024)
        // - Convert each Message to a map via Message.toMap() and set as "messages"
        // - If stream is true, add "stream": true
        // - Serialize to JSON string with ObjectMapper and return
        // - Wrap checked exceptions in RuntimeException
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("model", modelName);
            map.put("system", systemPrompt);
            map.put("max_tokens", 1024);

            if (stream) {
                map.put("stream", stream);
            }

            map.put("messages", messages.stream().map(Message::toMap).toList());
            return MAPPER.writeValueAsString(map);
        } catch (Exception e) {
            throw new RuntimeException("Error building request body", e);
        }
    }
}
