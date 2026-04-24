package t8.agent.task.agents;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import commons.Constants;
import commons.model.Message;
import commons.model.Role;
import t8.agent.task.tools.BaseTool;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OpenAIBasedAgent extends BaseAgent {

    private final String endpoint;
    private final List<JsonNode> toolsSchemas;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAIBasedAgent(String model, String apiKey, List<BaseTool> tools, String systemPrompt) {
        super(model, apiKey, tools, systemPrompt);
        this.apiKey = "Bearer " + apiKey;
        this.endpoint = Constants.OPENAI_CHAT_COMPLETIONS_ENDPOINT;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.toolsSchemas = tools != null
                ? tools.stream().map(t -> {
                    try { return objectMapper.readTree(t.getOpenAiSchema()); }
                    catch (Exception e) { throw new RuntimeException(e); }
                  }).collect(Collectors.toList())
                : List.of();

        try {
            System.out.println(endpoint);
            System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(toolsSchemas));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Message getResponse(List<Message> messages, boolean printRequest) {
        try {
            // Prepend system prompt for the API payload only — never stored in messages
            List<Map<String, Object>> requestMessages = new ArrayList<>();
            if (systemPrompt != null) {
                requestMessages.add(new Message(Role.SYSTEM, systemPrompt).toMap());
            }
            for (Message msg : messages) {
                requestMessages.add(msg.toMap());
            }

            Map<String, Object> requestData = new HashMap<>();
            requestData.put("model", model);
            requestData.put("messages", requestMessages);
            requestData.put("tools", toolsSchemas);

            String requestJson = objectMapper.writeValueAsString(requestData);

            if (printRequest) {
                System.out.println(endpoint);
                System.out.println("REQUEST: " + objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(Map.of("messages", requestMessages)));
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Authorization", apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> data = objectMapper.readValue(response.body(), Map.class);
                List<Map<String, Object>> choices = (List<Map<String, Object>>) data.get("choices");

                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    System.out.println("RESPONSE: " + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(choice));
                    System.out.println("-".repeat(100));

                    Map<String, Object> messageData = (Map<String, Object>) choice.get("message");
                    String content = (String) messageData.get("content");
                    List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) messageData.get("tool_calls");

                    Message aiResponse = new Message(Role.ASSISTANT, content, null, null, toolCalls);

                    if ("tool_calls".equals(choice.get("finish_reason"))) {
                        messages.add(aiResponse);
                        List<Message> toolMessages = processToolCalls(toolCalls);
                        messages.addAll(toolMessages);
                        return getResponse(messages, printRequest);
                    }

                    return aiResponse;
                }
                throw new RuntimeException("No Choice has been present in the response");
            } else {
                throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Message> processToolCalls(List<Map<String, Object>> toolCalls) throws IOException {
        List<Message> toolMessages = new ArrayList<>();
        for (Map<String, Object> toolCall : toolCalls) {
            String toolCallId = (String) toolCall.get("id");
            Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
            String functionName = (String) function.get("name");
            String argumentsJson = (String) function.get("arguments");
            Map<String, Object> arguments = objectMapper.readValue(argumentsJson, Map.class);

            String toolResult = callTool(functionName, arguments);

            toolMessages.add(new Message(Role.TOOL, toolResult, toolCallId, functionName, null));
            System.out.println("FUNCTION '" + functionName + "'\n" + toolResult + "\n" + "-".repeat(50));
        }
        return toolMessages;
    }
}
