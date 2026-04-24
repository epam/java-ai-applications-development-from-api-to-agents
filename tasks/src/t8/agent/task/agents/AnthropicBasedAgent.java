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

public class AnthropicBasedAgent extends BaseAgent {

    private final String endpoint;
    private final List<JsonNode> toolsSchemas;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AnthropicBasedAgent(String model, String apiKey, List<BaseTool> tools, String systemPrompt) {
        super(model, apiKey, tools, systemPrompt);
        this.endpoint = Constants.ANTHROPIC_ENDPOINT;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.toolsSchemas = tools != null
                ? tools.stream().map(t -> {
                    try { return objectMapper.readTree(t.getAnthropicSchema()); }
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
            List<Map<String, Object>> anthropicMessages = toAnthropicMessages(messages);

            Map<String, Object> requestData = new HashMap<>();
            requestData.put("model", model);
            requestData.put("max_tokens", 8096);
            requestData.put("messages", anthropicMessages);
            requestData.put("tools", toolsSchemas);
            if (systemPrompt != null) {
                requestData.put("system", systemPrompt);
            }

            String requestJson = objectMapper.writeValueAsString(requestData);

            if (printRequest) {
                System.out.println(endpoint);
                System.out.println("REQUEST: " + objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(Map.of("messages", anthropicMessages)));
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> data = objectMapper.readValue(response.body(), Map.class);

                List<Map<String, Object>> contentBlocks = (List<Map<String, Object>>) data.get("content");
                String stopReason = (String) data.get("stop_reason");

                System.out.println("RESPONSE: " + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data));
                System.out.println("-".repeat(100));

                String textContent = contentBlocks.stream()
                        .filter(b -> "text".equals(b.get("type")))
                        .map(b -> (String) b.get("text"))
                        .findFirst()
                        .orElse(null);

                List<Map<String, Object>> toolUseBlocks = contentBlocks.stream()
                        .filter(b -> "tool_use".equals(b.get("type")))
                        .collect(Collectors.toList());

                Message aiResponse = new Message(
                        Role.ASSISTANT,
                        textContent,
                        null,
                        null,
                        !toolUseBlocks.isEmpty() ? contentBlocks : null
                );

                if ("tool_use".equals(stopReason)) {
                    messages.add(aiResponse);
                    List<Message> toolMessages = processToolCalls(toolUseBlocks);
                    messages.addAll(toolMessages);
                    return getResponse(messages, printRequest);
                }

                return aiResponse;
            } else {
                throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Map<String, Object>> toAnthropicMessages(List<Message> messages) {
        List<Map<String, Object>> result = new ArrayList<>();
        int i = 0;
        while (i < messages.size()) {
            Message msg = messages.get(i);
            if (msg.role() == Role.TOOL) {
                List<Map<String, Object>> toolResults = new ArrayList<>();
                while (i < messages.size() && messages.get(i).role() == Role.TOOL) {
                    Message toolMsg = messages.get(i);
                    Map<String, Object> toolResult = new HashMap<>();
                    toolResult.put("type", "tool_result");
                    toolResult.put("tool_use_id", toolMsg.toolCallId());
                    toolResult.put("content", toolMsg.content());
                    toolResults.add(toolResult);
                    i++;
                }
                result.add(Map.of("role", "user", "content", toolResults));
            } else if (msg.role() == Role.ASSISTANT) {
                Map<String, Object> assistantMsg = new HashMap<>();
                assistantMsg.put("role", "assistant");
                assistantMsg.put("content", msg.toolCalls() != null ? msg.toolCalls() : msg.content());
                result.add(assistantMsg);
                i++;
            } else {
                result.add(Map.of("role", msg.role().getValue(), "content", msg.content()));
                i++;
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Message> processToolCalls(List<Map<String, Object>> toolUseBlocks) {
        List<Message> toolMessages = new ArrayList<>();
        for (Map<String, Object> block : toolUseBlocks) {
            String toolUseId = (String) block.get("id");
            String functionName = (String) block.get("name");
            Map<String, Object> arguments = (Map<String, Object>) block.get("input");

            String toolResult = callTool(functionName, arguments);

            toolMessages.add(new Message(Role.TOOL, toolResult, toolUseId, functionName, null));
            System.out.println("FUNCTION '" + functionName + "'\n" + toolResult + "\n" + "-".repeat(50));
        }
        return toolMessages;
    }
}
