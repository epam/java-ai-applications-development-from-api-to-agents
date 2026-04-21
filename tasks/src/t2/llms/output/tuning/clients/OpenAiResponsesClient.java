package t2.llms.output.tuning.clients;

import com.fasterxml.jackson.databind.JsonNode;
import commons.Constants;
import commons.model.Message;
import commons.model.Role;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OpenAiResponsesClient extends AIClient {

    public OpenAiResponsesClient(String modelName) {
        super(
                Constants.OPENAI_RESPONSES_ENDPOINT,
                modelName,
                "Bearer " + Constants.OPENAI_API_KEY,
                "Authorization"
        );
    }

    @Override
    public Message response(
            List<Message> messages,
            boolean printRequest,
            boolean printOnlyContent,
            Map<String, Object> options
    ) {
        var inputMessages = messages.stream().map(Message::toMap).toList();
        var headers = Map.of(
                "Authorization", apiKey,
                "Content-Type", "application/json"
        );
        var requestData = new LinkedHashMap<String, Object>();
        requestData.put("model", modelName);
        requestData.put("input", inputMessages);
        requestData.putAll(deepCopy(options));

        if (printRequest) {
            printRequest(requestData, headers);
        }

        var responseBody = post(endpoint, headers, requestData);
        try {
            var json = MAPPER.readTree(responseBody);
            var content = extractResponsesOutputText(json);
            printResponse(MAPPER.readValue(responseBody, Object.class), printOnlyContent, content);
            return new Message(Role.ASSISTANT, content);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse OpenAI Responses response", e);
        }
    }

    private String extractResponsesOutputText(JsonNode root) {
        var direct = root.path("output_text");
        if (direct.isTextual()) {
            return direct.asText();
        }

        for (var outputItem : root.path("output")) {
            if (!"message".equals(outputItem.path("type").asText())) {
                continue;
            }
            for (var contentItem : outputItem.path("content")) {
                if ("output_text".equals(contentItem.path("type").asText())) {
                    return contentItem.path("text").asText();
                }
            }
        }

        throw new IllegalStateException("No output_text present in the response");
    }
}
