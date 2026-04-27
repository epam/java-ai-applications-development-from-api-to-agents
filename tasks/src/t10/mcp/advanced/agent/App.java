package t10.mcp.advanced.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import commons.Constants;
import t10.mcp.advanced.agent.clients.CustomMcpClient;
import t10.mcp.advanced.agent.clients.McpClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class App {

    public static void main(String[] args) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        // Step 1: Use McpClient (stream-based SSE parsing, Java HttpClient)
        McpClient mcpClient = McpClient.create("http://localhost:8006/mcp");
        List<Map<String, Object>> tools = mcpClient.getTools();

        // Step 2: Switch to CustomMcpClient (string-based SSE parsing, explicit state)
        // CustomMcpClient mcpClient = CustomMcpClient.create("http://localhost:8006/mcp");
        // List<Map<String, Object>> tools = mcpClient.getTools();

        tools.forEach(tool -> {
            try {
                System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(tool));
            } catch (Exception e) {
                System.out.println(tool);
            }
        });

        Agent agent = new Agent(
                Constants.OPENAI_API_KEY,
                Constants.GPT_5_4,
                tools,
                mcpClient
        );

        List<ChatCompletionMessageParam> messages = new ArrayList<>();
        messages.add(ChatCompletionMessageParam.ofSystem(
                ChatCompletionSystemMessageParam.builder()
                        .content("You are an advanced AI agent. Your goal is to assist user with his questions.")
                        .build()
        ));

        Scanner scanner = new Scanner(System.in);
        System.out.println("MCP-based Agent is ready! Type your query or 'exit' to exit.");

        while (true) {
            System.out.print("\n> ");
            String input = scanner.nextLine().strip();
            if ("exit".equalsIgnoreCase(input)) break;

            messages.add(ChatCompletionMessageParam.ofUser(
                    ChatCompletionUserMessageParam.builder()
                            .content(input)
                            .build()
            ));

            ChatCompletionMessageParam response = agent.getCompletion(messages);
            messages.add(response);
        }
    }
}

// Check if Arkadiy Dobkin present as a user, if not then search info about him in the web and add him
