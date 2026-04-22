package t5.rag.advanced.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import commons.model.Message;
import commons.model.Role;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class ChatCompletionClient {

    private final String endpoint;
    private final String apiKey;
    private final String modelName;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ChatCompletionClient(String endpoint, String modelName, String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("API key cannot be null or empty");
        }
        this.endpoint = endpoint;
        this.apiKey = "Bearer " + apiKey;
        this.modelName = modelName;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public Message getCompletion(List<Message> messages) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", modelName);

            ArrayNode messagesArray = objectMapper.createArrayNode();
            for (Message msg : messages) {
                ObjectNode msgNode = objectMapper.createObjectNode();
                msgNode.put("role", msg.role().getValue());
                msgNode.put("content", msg.content());
                messagesArray.add(msgNode);
            }
            requestBody.set("messages", messagesArray);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Authorization", apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode responseJson = objectMapper.readTree(response.body());
                JsonNode choices = responseJson.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    String content = choices.get(0).get("message").get("content").asText();
                    return new Message(Role.ASSISTANT, content);
                }
                throw new RuntimeException("No Choice has been present in the response");
            }
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
