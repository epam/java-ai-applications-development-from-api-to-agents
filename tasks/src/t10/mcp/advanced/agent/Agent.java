package t10.mcp.advanced.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonValue;
import com.openai.helpers.ChatCompletionAccumulator;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessage;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionMessageToolCall;
import com.openai.models.chat.completions.ChatCompletionTool;
import com.openai.models.chat.completions.ChatCompletionToolMessageParam;

import t10.mcp.advanced.agent.clients.BaseMcpClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class Agent {

    private final String model;
    private final List<ChatCompletionTool> tools;
    private final BaseMcpClient mcpClient;
    private final OpenAIClient openAIClient;
    private final ObjectMapper objectMapper;

    public Agent(String apiKey, String model, List<Map<String, Object>> tools, BaseMcpClient mcpClient) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("API key cannot be null or empty");
        }
        this.model = model;
        this.mcpClient = mcpClient;
        this.objectMapper = new ObjectMapper();
        this.openAIClient = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
        this.tools = tools.stream().map(this::convertTool).toList();
    }

    public ChatCompletionMessageParam getCompletion(List<ChatCompletionMessageParam> messages) {
        ChatCompletionMessage responseMsg = streamCompletion(messages);
        List<ChatCompletionMessageToolCall> toolCalls = responseMsg.toolCalls().orElse(List.of());

        ChatCompletionMessageParam assistantParam = buildAssistantParam(responseMsg, toolCalls);

        if (!toolCalls.isEmpty()) {
            messages.add(assistantParam);
            processToolCalls(toolCalls, messages);
            return getCompletion(messages);
        }

        return assistantParam;
    }

    private ChatCompletionMessageParam buildAssistantParam(ChatCompletionMessage msg,
                                                           List<ChatCompletionMessageToolCall> toolCalls) {
        var builder = ChatCompletionAssistantMessageParam.builder();
        msg.content().ifPresent(builder::content);
        if (!toolCalls.isEmpty()) {
            builder.toolCalls(toolCalls);
        }
        return ChatCompletionMessageParam.ofAssistant(builder.build());
    }

    private ChatCompletionMessage streamCompletion(List<ChatCompletionMessageParam> messages) {
        var params = ChatCompletionCreateParams.builder()
                .model(model)
                .tools(tools)
                .messages(messages)
                .build();
        var accumulator = ChatCompletionAccumulator.create();

        System.out.print("🤖: ");

        try (var stream = openAIClient.chat().completions().createStreaming(params)) {
            stream.stream().forEach(chunk -> {
                accumulator.accumulate(chunk);
                if (!chunk.choices().isEmpty()) {
                    chunk.choices().getFirst().delta().content().ifPresent(System.out::print);
                }
            });
        }

        System.out.println();

        return accumulator.chatCompletion().choices().getFirst().message();
    }

    @SuppressWarnings("unchecked")
    private void processToolCalls(List<ChatCompletionMessageToolCall> toolCalls, List<ChatCompletionMessageParam> messages) {
        for (ChatCompletionMessageToolCall toolCall : toolCalls) {
            String toolCallId = toolCall.id();
            String functionName = toolCall.function().name();
            String argumentsJson = toolCall.function().arguments();

            Map<String, Object> arguments;
            try {
                arguments = objectMapper.readValue(argumentsJson, Map.class);
            } catch (IOException e) {
                throw new RuntimeException("Failed to parse tool arguments", e);
            }

            String toolResult;
            try {
                toolResult = mcpClient.callTool(functionName, arguments);
            } catch (Exception e) {
                toolResult = "Error calling tool '" + functionName + "': " + e.getMessage();
                System.out.println(toolResult);
            }

            messages.add(ChatCompletionMessageParam.ofTool(
                    ChatCompletionToolMessageParam.builder()
                            .toolCallId(toolCallId)
                            .content(toolResult)
                            .build()
            ));
        }
    }

    @SuppressWarnings("unchecked")
    private ChatCompletionTool convertTool(Map<String, Object> toolMap) {
        Map<String, Object> function = (Map<String, Object>) toolMap.get("function");
        String name = (String) function.get("name");
        String description = (String) function.getOrDefault("description", "");
        Map<String, Object> parameters = (Map<String, Object>) function.get("parameters");

        var paramsBuilder = FunctionParameters.builder();
        if (parameters != null) {
            parameters.forEach((k, v) -> paramsBuilder.putAdditionalProperty(k, JsonValue.from(v)));
        }

        return ChatCompletionTool.builder()
                .type(JsonValue.from("function"))
                .function(FunctionDefinition.builder()
                        .name(name)
                        .description(description)
                        .parameters(paramsBuilder.build())
                        .build())
                .build();
    }
}
