package t5.rag.advanced.embeddings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmbeddingsClient {

    private final String endpoint;
    private final String apiKey;
    private final String modelName;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public EmbeddingsClient(String endpoint, String modelName, String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("API key cannot be null or empty");
        }
        this.endpoint = endpoint;
        this.apiKey = "Bearer " + apiKey;
        this.modelName = modelName;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Generate indexed embeddings for a single input string.
     * Returns Map where key 0 holds the embedding vector.
     */
    public Map<Integer, List<Float>> getEmbeddings(String input, int dimensions) {
        return getEmbeddings(List.of(input), dimensions);
    }

    /**
     * Generate indexed embeddings for a list of input strings.
     * Returns Map: inputs[0] -> [0][embedding], inputs[1] -> [1][embedding], ...
     */
    public Map<Integer, List<Float>> getEmbeddings(List<String> inputs, int dimensions) {
        System.out.println("Creating embeddings for `" + inputs + "` \nAnd such dimensions: " + dimensions + "\n📋Results:\n");

        try {
            String requestBody = String.format(
                    """
                            {
                                    "model": "%s",
                                    "dimensions": %d,
                                    "input": %s
                            }
                            """,
                    modelName, dimensions, objectMapper.writeValueAsString(inputs)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Authorization", apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode responseJson = objectMapper.readTree(response.body());
                return fromData(responseJson.get("data"));
            }
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map<Integer, List<Float>> fromData(JsonNode data) {
        Map<Integer, List<Float>> result = new HashMap<>();
        for (JsonNode embeddingObj : data) {
            int index = embeddingObj.get("index").asInt();
            List<Float> embedding = new ArrayList<>();
            for (JsonNode value : embeddingObj.get("embedding")) {
                embedding.add((float) value.asDouble());
            }
            result.put(index, embedding);
        }
        return result;
    }
}
