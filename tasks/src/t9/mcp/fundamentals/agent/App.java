package t9.mcp.fundamentals.agent;

import commons.Constants;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import io.modelcontextprotocol.spec.McpSchema;
import t9.mcp.fundamentals.agent.mcp.client.BaseClient;
import t9.mcp.fundamentals.agent.mcp.client.HttpClient;
import t9.mcp.fundamentals.agent.mcp.client.StdioClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class App {

    // Classpath of the compiled project for launching the STDIO server as a subprocess
    private static final String STDIO_SERVER_CLASS = "t9.mcp.fundamentals.mcp.server.StdioServerApp";

    public static void main(String[] args) throws Exception {
        // Switch active client by commenting/uncommenting:

        String javaClasspath = System.getProperty("java.class.path");

        // --- HTTP client (start HttpServer.java first): ---
        try (BaseClient mcpClient = new HttpClient("http://localhost:8005/mcp")) {
            runAgent(mcpClient);
        }

//        try (BaseClient mcpClient = new StdioClient(
//                null,
//                "java",
//                List.of("-cp", javaClasspath, STDIO_SERVER_CLASS),
//                null
//        )) {
//            runAgent(mcpClient);
//        }

        // --- Docker STDIO client: ---
//        try (BaseClient mcpClient = new StdioClient("mcp/duckduckgo:latest", null, null, null)) {
//            runAgent(mcpClient);
//        }
    }

    private static void runAgent(BaseClient mcpClient) {
        mcpClient.connect();

        System.out.println("\n=== Available Resources ===");
        List<McpSchema.Resource> resources = mcpClient.getResources();
        resources.forEach(r -> System.out.println("  " + r.uri() + " — " + r.description()));

        System.out.println("\n=== Available Tools ===");
        List<Map<String, Object>> tools = mcpClient.getTools();
        tools.forEach(t -> {
            Map<?, ?> fn = (Map<?, ?>) t.get("function");
            System.out.println("  " + fn.get("name") + ": " + fn.get("description"));
        });

        List<ChatCompletionMessageParam> messages = new ArrayList<>();
        messages.add(ChatCompletionMessageParam.ofSystem(
                ChatCompletionSystemMessageParam.builder().content(Prompts.SYSTEM_PROMPT).build()
        ));

        System.out.println("\n=== Available Prompts ===");
        List<McpSchema.Prompt> prompts = mcpClient.getPrompts();
        for (McpSchema.Prompt prompt : prompts) {
            System.out.println("  " + prompt.name() + ": " + prompt.description());
            String content = mcpClient.getPrompt(prompt.name());
            System.out.println(content);
            messages.add(ChatCompletionMessageParam.ofUser(
                    ChatCompletionUserMessageParam.builder()
                            .content("## Prompt provided by MCP server:\n" + prompt.description() + "\n" + content)
                            .build()
            ));
        }

        Agent agent = new Agent(
                Constants.OPENAI_API_KEY,
                Constants.GPT_5_4,
                tools,
                mcpClient
        );

        System.out.println("\nMCP-based Agent is ready! Type your query or 'exit' to exit.");
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("\n> ");
            String userInput = scanner.nextLine().strip();

            if ("exit".equalsIgnoreCase(userInput)) {
                System.out.println("Exiting. Goodbye!");
                break;
            }

            messages.add(ChatCompletionMessageParam.ofUser(
                    ChatCompletionUserMessageParam.builder().content(userInput).build()
            ));

            ChatCompletionMessageParam aiMessage = agent.getCompletion(messages);
            messages.add(aiMessage);
        }
    }
}
