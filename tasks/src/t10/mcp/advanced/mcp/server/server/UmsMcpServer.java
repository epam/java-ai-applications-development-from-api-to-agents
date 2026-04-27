package t10.mcp.advanced.mcp.server.server;

import commons.user.service.UserServiceClient;
import org.springframework.stereotype.Service;
import t10.mcp.advanced.mcp.server.server.models.McpRequest;
import t10.mcp.advanced.mcp.server.server.models.McpResponse;
import t10.mcp.advanced.mcp.server.server.tools.BaseTool;
import t10.mcp.advanced.mcp.server.server.tools.users.CreateUserTool;
import t10.mcp.advanced.mcp.server.server.tools.users.DeleteUserTool;
import t10.mcp.advanced.mcp.server.server.tools.users.GetUserByIdTool;
import t10.mcp.advanced.mcp.server.server.tools.users.SearchUsersTool;
import t10.mcp.advanced.mcp.server.server.tools.users.UpdateUserTool;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UmsMcpServer {

    private static final String PROTOCOL_VERSION = "2025-11-25";

    private final Map<String, McpSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, BaseTool> tools = new LinkedHashMap<>();

    public UmsMcpServer() {
        registerTools();
    }

    private void registerTools() {
        UserServiceClient client = new UserServiceClient();
        List.of(
                new GetUserByIdTool(client),
                new SearchUsersTool(client),
                new CreateUserTool(client),
                new UpdateUserTool(client),
                new DeleteUserTool(client)
        ).forEach(tool -> tools.put(tool.getName(), tool));
    }

    public McpSession getSession(String sessionId) {
        McpSession session = sessions.get(sessionId);
        if (session != null) {
            session.updateActivity();
        }
        return session;
    }

    public record InitResult(McpResponse response, String sessionId) {}

    public InitResult handleInitialize(McpRequest request) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        sessions.put(sessionId, new McpSession(sessionId));

        String protocolVersion = Optional.ofNullable(request.getParams())
                .map(p -> (String) p.get("protocolVersion"))
                .orElse(PROTOCOL_VERSION);

        Map<String, Object> capabilities = new LinkedHashMap<>();
        capabilities.put("tools", Map.of("listChanged", false));
        capabilities.put("resources", null);
        capabilities.put("prompts", null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("protocolVersion", protocolVersion);
        result.put("capabilities", capabilities);
        result.put("serverInfo", Map.of("name", "custom-ums-mcp-server", "version", "1.0.0"));

        return new InitResult(new McpResponse(request.getId(), result), sessionId);
    }

    public McpResponse handleToolsList(McpRequest request) {
        List<Map<String, Object>> toolsList = tools.values().stream()
                .map(BaseTool::toMcpTool)
                .toList();
        return new McpResponse(request.getId(), Map.of("tools", toolsList));
    }

    public McpResponse handleToolsCall(McpRequest request) {
        Map<String, Object> params = request.getParams();

        if (params == null) {
            return new McpResponse(request.getId(),
                    new McpResponse.McpError(-32602, "Missing parameters"));
        }

        String toolName = (String) params.get("name");
        if (toolName == null || toolName.isBlank()) {
            return new McpResponse(request.getId(),
                    new McpResponse.McpError(-32602, "Missing required parameter: name"));
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) params.getOrDefault("arguments", Map.of());
        System.out.println(request.getMethod() + " | tool=" + toolName + " | args=" + arguments);

        BaseTool tool = tools.get(toolName);
        if (tool == null) {
            return new McpResponse(request.getId(),
                    new McpResponse.McpError(-32601, "Tool '" + toolName + "' not found"));
        }

        try {
            String resultText = tool.execute(arguments);
            return new McpResponse(request.getId(), Map.of(
                    "content", List.of(Map.of("type", "text", "text", resultText))
            ));
        } catch (Exception e) {
            return new McpResponse(request.getId(), Map.of(
                    "content", List.of(Map.of("type", "text", "text", "Tool execution error: " + e.getMessage())),
                    "isError", true
            ));
        }
    }
}
