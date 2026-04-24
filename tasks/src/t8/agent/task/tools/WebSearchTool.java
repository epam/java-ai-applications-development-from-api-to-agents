package t8.agent.task.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import commons.Constants;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public class WebSearchTool extends BaseTool {

    private final String apiKey;
    private final String endpoint;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public WebSearchTool(String openAiApiKey) {
        this.apiKey = "Bearer " + openAiApiKey;
        this.endpoint = Constants.OPENAI_RESPONSES_ENDPOINT;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getName() {
        return "web_search_tool";
    }

    @Override
    public String getDescription() {
        return "Tool for WEB searching.";
    }

    @Override
    public String getInputSchema() {
        return """
                {
                    "type": "object",
                    "properties": {
                        "request": {
                            "type": "string",
                            "description": "The search query or question to search for on the web"
                        }
                    },
                    "required": ["request"]
                }
                """;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String execute(Map<String, Object> arguments) {
        try {
            Map<String, Object> requestData = Map.of(
                    "model", "gpt-5.2",
                    "tools", List.of(Map.of("type", "web_search")),
                    "input", String.valueOf(arguments.get("request"))
            );

            String json = objectMapper.writeValueAsString(requestData);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Authorization", apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> data = objectMapper.readValue(response.body(), Map.class);
                List<Map<String, Object>> output = (List<Map<String, Object>>) data.get("output");
                if (output != null) {
                    for (Map<String, Object> item : output) {
                        if ("message".equals(item.get("type"))) {
                            List<Map<String, Object>> content = (List<Map<String, Object>>) item.get("content");
                            if (content != null) {
                                for (Map<String, Object> block : content) {
                                    if ("output_text".equals(block.get("type"))) {
                                        return (String) block.get("text");
                                    }
                                }
                            }
                        }
                    }
                }
                return "No result returned from web search.";
            }
            return "Error: " + response.statusCode() + " " + response.body();
        } catch (IOException | InterruptedException e) {
            return "Error: " + e.getMessage();
        }
    }
}
