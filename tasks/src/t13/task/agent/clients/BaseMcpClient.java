package t13.task.agent.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import t13.task.agent.models.McpToolModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class BaseMcpClient implements AutoCloseable {

    protected McpSyncClient mcpClient;
    protected final ObjectMapper objectMapper = new ObjectMapper();
    private ToolCallback[] toolCallbacks;

    public abstract void connect();

    /**
     * Must be called by subclasses in {@link #connect()} after {@code mcpClient.initialize()}.
     * Fetches available tools from the MCP server and caches them as Spring AI {@link ToolCallback}s.
     */
    protected void initToolCallbackProvider() {
        SyncMcpToolCallbackProvider provider = SyncMcpToolCallbackProvider.builder()
                .addMcpClient(mcpClient)
                .build();
        this.toolCallbacks = provider.getToolCallbacks();
    }

    public List<Map<String, Object>> getTools() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ToolCallback cb : toolCallbacks) {
            var def = cb.getToolDefinition();
            try {
                Map<String, Object> tool = Map.of(
                        "type", "function",
                        "function", Map.of(
                                "name", def.name(),
                                "description", def.description() != null ? def.description() : "",
                                "parameters", objectMapper.readValue(def.inputSchema(), Map.class)
                        )
                );
                result.add(tool);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    public List<McpToolModel> getMcpTools() {
        List<McpToolModel> result = new ArrayList<>();
        for (ToolCallback cb : toolCallbacks) {
            var def = cb.getToolDefinition();
            try {
                Map<String, Object> params = objectMapper.readValue(def.inputSchema(), Map.class);
                result.add(new McpToolModel(def.name(), def.description() != null ? def.description() : "", params));
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse tool schema for: " + def.name(), e);
            }
        }
        return result;
    }

    public String callTool(String toolName, Map<String, Object> toolArgs) {
        System.out.println("    🔧 Calling `" + toolName + "` with " + toolArgs);

        ToolCallback callback = Arrays.stream(toolCallbacks)
                .filter(cb -> cb.getToolDefinition().name().equals(toolName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Tool not found: " + toolName));

        try {
            String result = callback.call(objectMapper.writeValueAsString(toolArgs));
            System.out.println("    ⚙️: " + result + "\n");
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to call tool '" + toolName + "': " + e.getMessage(), e);
        }
    }

    public List<McpSchema.Resource> getResources() {
        try {
            return mcpClient.listResources().resources();
        } catch (Exception e) {
            System.out.println("Server doesn't support list_resources: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public String getResource(String uri) {
        McpSchema.ReadResourceResult result = mcpClient.readResource(
                new McpSchema.ReadResourceRequest(uri)
        );
        if (result.contents() == null || result.contents().isEmpty()) {
            return "";
        }
        McpSchema.ResourceContents contents = result.contents().get(0);
        if (contents instanceof McpSchema.TextResourceContents tc) {
            return tc.text();
        } else if (contents instanceof McpSchema.BlobResourceContents bc) {
            return bc.blob();
        }
        return contents.toString();
    }

    public List<McpSchema.Prompt> getPrompts() {
        try {
            return mcpClient.listPrompts().prompts();
        } catch (Exception e) {
            System.out.println("Server doesn't support list_prompts: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public String getPrompt(String name) {
        McpSchema.GetPromptResult result = mcpClient.getPrompt(
                new McpSchema.GetPromptRequest(name, null)
        );
        if (result.messages() == null || result.messages().isEmpty()) {
            return "";
        }
        StringBuilder combined = new StringBuilder();
        for (McpSchema.PromptMessage msg : result.messages()) {
            if (msg.content() instanceof McpSchema.TextContent tc) {
                combined.append(tc.text()).append("\n");
            }
        }
        return combined.toString().strip();
    }

    @Override
    public void close() {
        if (mcpClient != null) {
            mcpClient.closeGracefully();
        }
    }
}
