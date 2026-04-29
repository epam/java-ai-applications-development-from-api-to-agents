package t13.task.agent.clients;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;

import java.util.List;
import java.util.Map;

/**
 * MCP client that communicates via stdio with a locally spawned process.
 *
 * Supports two launch modes:
 *   1. Docker image  — pass dockerImage="mcp/duckduckgo:latest"
 *   2. Local command — pass command="java", args=["-cp", "...", "MainClass"]
 *
 * Usage:
 *   // Docker
 *   try (StdioClient client = new StdioClient("mcp/duckduckgo:latest", null, null, null)) { ... }
 *
 *   // Local Java process
 *   try (StdioClient client = new StdioClient(null, "java", List.of("-cp", "...", "StdioServer"), null)) { ... }
 */
public class StdioMcpClient extends BaseMcpClient {

    private final String dockerImage;
    private final String command;
    private final List<String> args;
    private final Map<String, String> env;

    public StdioMcpClient(String dockerImage, String command, List<String> args, Map<String, String> env) {
        if (dockerImage == null && command == null) {
            throw new IllegalArgumentException("Provide either 'dockerImage' or 'command' to launch the MCP server.");
        }
        this.dockerImage = dockerImage;
        this.command = command;
        this.args = args != null ? args : List.of();
        this.env = env;
    }

    @Override
    public void connect() {
        ServerParameters params = buildServerParameters();
        System.out.println(startupMessage());

        StdioClientTransport transport = new StdioClientTransport(
                params,
                new JacksonMcpJsonMapper(objectMapper)
        );

        mcpClient = McpClient.sync(transport).build();

        System.out.println("Initializing MCP session...");
        mcpClient.initialize();
        initToolCallbackProvider();
        System.out.println("MCP session initialized.");
    }

    private ServerParameters buildServerParameters() {
        if (dockerImage != null) {
            return ServerParameters.builder("docker")
                    .args("run", "--rm", "-i", dockerImage)
                    .env(env)
                    .build();
        }
        return ServerParameters.builder(command)
                .args(args.toArray(new String[0]))
                .env(env)
                .build();
    }

    private String startupMessage() {
        if (dockerImage != null) {
            return "Starting Docker container: " + dockerImage
                    + "\nTo inspect: docker ps --filter 'ancestor=" + dockerImage + "'";
        }
        return "Starting local stdio server: " + command + " " + String.join(" ", args);
    }
}
