package t2.llms.output.tuning.clients;

import com.fasterxml.jackson.databind.JsonNode;
import commons.Constants;
import commons.model.Message;
import commons.model.Role;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AnthropicAiClient extends AIClient {

    public AnthropicAiClient(String modelName) {
        super(
                Constants.ANTHROPIC_ENDPOINT,
                modelName,
                Constants.ANTHROPIC_API_KEY,
                "x-api-key"
        );
    }

    @Override
    public Message response(
            List<Message> messages,
            boolean printRequest,
            boolean printOnlyContent,
            Map<String, Object> options
    ) {
        var headers = Map.of(
                "x-api-key", apiKey,
                "Content-Type", "application/json",
                "anthropic-version", "2023-06-01"
        );
        var requestData = new LinkedHashMap<String, Object>();
        requestData.put("model", modelName);
        requestData.put("max_tokens", options.getOrDefault("max_tokens", 1024));
        requestData.put("messages", messages.stream().map(Message::toMap).toList());
        requestData.putAll(deepCopy(options));

        if (printRequest) {
            printRequest(requestData, headers);
        }

        var responseBody = post(endpoint, headers, requestData);
        try {
            var json = MAPPER.readTree(responseBody);
            var content = extractAnthropicText(json);
            printResponse(MAPPER.readValue(responseBody, Object.class), printOnlyContent, content);
            return new Message(Role.ASSISTANT, content);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse Anthropic response", e);
        }
    }

    private String extractAnthropicText(JsonNode root) {
        var content = new StringBuilder();
        for (var block : root.path("content")) {
            if ("text".equals(block.path("type").asText())) {
                content.append(block.path("text").asText(""));
            }
        }
        if (content.isEmpty()) {
            throw new IllegalStateException("No content blocks present in the response");
        }
        return content.toString();
    }
}
