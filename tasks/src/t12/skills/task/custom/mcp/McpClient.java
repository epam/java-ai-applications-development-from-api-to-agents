package t12.skills.task.custom.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;
import java.util.Map;

/**
 * Thin wrapper around McpSyncClient — connects to the remote MCP server and
 * delegates listTools / callTool to it directly, mirroring T12MCPClient in
 * the Python version. Local tools (read_skill, execute_code script resolution)
 * are owned by their respective BaseTool subclasses.
 */
public class McpClient implements AutoCloseable {

    private final String mcpServerUrl;
    private McpSyncClient mcpSyncClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public McpClient(String mcpServerUrl) {
        this.mcpServerUrl = mcpServerUrl;
    }

    public void connect() {
        String normalizedUrl = mcpServerUrl.endsWith("/")
                ? mcpServerUrl.substring(0, mcpServerUrl.length() - 1)
                : mcpServerUrl;
        int lastSlash = normalizedUrl.lastIndexOf('/');
        String baseUrl;
        String endpoint;
        if (lastSlash > 7) {
            baseUrl = normalizedUrl.substring(0, lastSlash);
            endpoint = normalizedUrl.substring(lastSlash) + "/";
        } else {
            baseUrl = normalizedUrl;
            endpoint = "/mcp/";
        }

        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport
                .builder(baseUrl)
                .endpoint(endpoint)
                .jsonMapper(new JacksonMcpJsonMapper(objectMapper))
                .build();

        mcpSyncClient = io.modelcontextprotocol.client.McpClient.sync(transport).build();
        System.out.println("Connecting to MCP server at " + mcpServerUrl + "...");
        mcpSyncClient.initialize();
        System.out.println("Connected to MCP server.");
    }

    /**
     * Returns tool models fetched from the MCP server, mirroring get_tools() / list_tools()
     * in the Python version. Schema augmentation (e.g. script_path) is the responsibility
     * of the BaseTool that wraps the MCP call.
     */
    @SuppressWarnings("unchecked")
    public List<McpToolModel> listTools() {
        return mcpSyncClient.listTools().tools().stream()
                .map(tool -> new McpToolModel(
                        tool.name(),
                        tool.description() != null ? tool.description() : "",
                        objectMapper.convertValue(tool.inputSchema(), Map.class)
                ))
                .toList();
    }

    /**
     * Calls a tool on the MCP server, mirroring session.call_tool() in the Python version.
     */
    public String callTool(String toolName, Map<String, Object> arguments) {
        McpSchema.CallToolResult callResult = mcpSyncClient.callTool(
                new McpSchema.CallToolRequest(toolName, arguments)
        );

        if (callResult.content() == null || callResult.content().isEmpty()) {
            return "";
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> contentMap = objectMapper.convertValue(callResult.content().get(0), Map.class);
        return (String) contentMap.getOrDefault("text", "");
    }

    @Override
    public void close() {
        if (mcpSyncClient != null) {
            mcpSyncClient.closeGracefully();
        }
    }
}
