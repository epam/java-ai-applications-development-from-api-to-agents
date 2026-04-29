package t11.mcp.auth.agent;

import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import commons.Constants;
import io.modelcontextprotocol.spec.McpSchema;
import t11.mcp.auth.agent.clients.ApiKeyMcpClient;
import t11.mcp.auth.agent.clients.BaseMcpClient;
import t11.mcp.auth.agent.clients.OAuthMcpClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class App {

    private static final String MCP_API_KEY = "dev-secret-key";

    public static void main(String[] args) throws Exception {

        //TODO:
        // Part 1 — API Key auth (after implementing ApiKey.java and ApiKeyMcpClient.java):
        //   1. Uncomment the ApiKeyMcpClient line below and comment out the OAuthMcpClient line
        //   2. Run ApiKeyServerApp.java first (listens on port 8007)
        //   3. Run this App — verify the agent connects and responds to queries
        //   4. Try removing the key or using a wrong key — the server should return 401
        //
        // Part 2 — OAuth 2.0 + PKCE (after implementing OAuth.java and OAuthMcpClient.java):
        //   1. Uncomment the OAuthMcpClient line below and comment out the ApiKeyMcpClient line
        //   2. Run OAuthServerApp.java first (listens on port 8008)
        //   3. Run this App — a browser will open for Keycloak login
        //   4. Test with mcp-user / password → agent works normally
        //   5. Test with no-access-user / password → server returns 403 Forbidden
        //   6. Wait 60 s between queries to observe automatic token refresh
        BaseMcpClient mcpClient = null;
//        mcpClient = new ApiKeyMcpClient("http://localhost:8007/mcp", MCP_API_KEY);
//        mcpClient = new OAuthMcpClient("http://localhost:8008/mcp");

        try (mcpClient) {

            mcpClient.connect();

            System.out.println("\n=== Available Tools ===");
            List<Map<String, Object>> tools = mcpClient.getTools();
            tools.forEach(t -> {
                Map<?, ?> fn = (Map<?, ?>) t.get("function");
                System.out.println("  " + fn.get("name") + ": " + fn.get("description"));
            });

            List<ChatCompletionMessageParam> messages = new ArrayList<>();

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
}
