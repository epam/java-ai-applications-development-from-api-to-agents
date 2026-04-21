package t2.llms.output.tuning.clients;

import com.fasterxml.jackson.databind.JsonNode;
import commons.Constants;
import commons.model.Message;
import commons.model.Role;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GeminiAiClient extends AIClient {

    public GeminiAiClient(String modelName) {
        super(
                Constants.GEMINI_ENDPOINT,
                modelName,
                Constants.GEMINI_API_KEY,
                "x-goog-api-key"
        );
    }

    @Override
    public Message response(
            List<Message> messages,
            boolean printRequest,
            boolean printOnlyContent,
            Map<String, Object> options
    ) {
        var url = endpoint + "/" + modelName + ":generateContent";
        var headers = Map.of(
                "Content-Type", "application/json",
                "x-goog-api-key", apiKey
        );

        var generationConfig = options.containsKey("generationConfig")
                ? deepCopy(castMap(options.get("generationConfig")))
                : new LinkedHashMap<String, Object>();
        generationConfig.putIfAbsent("maxOutputTokens", 1024);

        var requestData = new LinkedHashMap<String, Object>();
        requestData.put("contents", toGeminiContents(messages));
        requestData.put("generationConfig", generationConfig);

        if (printRequest) {
            printRequest(requestData, headers);
        }

        var responseBody = post(url, headers, requestData);
        try {
            var json = MAPPER.readTree(responseBody);
            var candidates = json.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                throw new IllegalStateException("No candidates present in the response");
            }

            var content = extractGeminiText(candidates.get(0).path("content").path("parts"));
            printResponse(MAPPER.readValue(responseBody, Object.class), printOnlyContent, content);
            return new Message(Role.ASSISTANT, content);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse Gemini response", e);
        }
    }

    private List<Map<String, Object>> toGeminiContents(List<Message> messages) {
        var contents = new ArrayList<Map<String, Object>>();
        for (var message : messages) {
            contents.add(new LinkedHashMap<>(Map.of(
                    "role", toGeminiRole(message.role()),
                    "parts", List.of(Map.of("text", message.content()))
            )));
        }
        return contents;
    }

    private String toGeminiRole(Role role) {
        return role == Role.ASSISTANT ? "model" : role.getValue();
    }

    private String extractGeminiText(JsonNode parts) {
        var content = new StringBuilder();
        for (var part : parts) {
            content.append(part.path("text").asText(""));
        }
        return content.toString();
    }
}
