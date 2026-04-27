package t10.mcp.advanced.mcp.server.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import t10.mcp.advanced.mcp.server.server.models.McpRequest;
import t10.mcp.advanced.mcp.server.server.models.McpResponse;

import java.io.IOException;
import java.io.PrintWriter;

@RestController
public class Server {

    private static final String SESSION_ID_HEADER = "Mcp-Session-Id";

    private final UmsMcpServer mcpServer;
    private final ObjectMapper objectMapper;

    public Server(UmsMcpServer mcpServer) {
        this.mcpServer = mcpServer;
        this.objectMapper = new ObjectMapper();
    }

    @PostMapping(value = "/mcp", consumes = "application/json")
    public void handleMcp(
            @RequestBody McpRequest request,
            @RequestHeader(value = "Accept", required = false) String accept,
            @RequestHeader(value = SESSION_ID_HEADER, required = false) String sessionId,
            HttpServletResponse httpResponse
    ) throws IOException {

        if (!isValidAcceptHeader(accept)) {
            writeError(httpResponse, 406, new McpResponse("server-error",
                    new McpResponse.McpError(-32600,
                            "Client must accept both application/json and text/event-stream")));
            return;
        }

        if ("initialize".equals(request.getMethod())) {
            UmsMcpServer.InitResult result = mcpServer.handleInitialize(request);
            writeSse(httpResponse, result.response(), result.sessionId());
            return;
        }

        if (sessionId == null || sessionId.isBlank()) {
            writeError(httpResponse, 400, new McpResponse("server-error",
                    new McpResponse.McpError(-32600, "Missing session ID")));
            return;
        }

        McpSession session = mcpServer.getSession(sessionId);
        if (session == null) {
            writeError(httpResponse, 400, new McpResponse("server-error",
                    new McpResponse.McpError(-32600, "No valid session ID provided")));
            return;
        }

        if ("notifications/initialized".equals(request.getMethod())) {
            session.setReadyForOperation(true);
            System.out.println("Client initialization complete");
            httpResponse.setStatus(202);
            httpResponse.setHeader(SESSION_ID_HEADER, sessionId);
            return;
        }

        if (!session.isReadyForOperation()) {
            writeError(httpResponse, 400, new McpResponse("server-error",
                    new McpResponse.McpError(-32600, "Session not ready for operations")));
            return;
        }

        McpResponse response = switch (request.getMethod()) {
            case "tools/list" -> mcpServer.handleToolsList(request);
            case "tools/call" -> mcpServer.handleToolsCall(request);
            default -> new McpResponse(
                    request.getId(),
                    new McpResponse.McpError(-32602, "Method '" + request.getMethod() + "' not found")
            );
        };

        writeSse(httpResponse, response, sessionId);
    }

    private void writeSse(HttpServletResponse httpResponse, McpResponse response, String sessionId)
            throws IOException {
        httpResponse.setContentType("text/event-stream");
        httpResponse.setCharacterEncoding("UTF-8");
        httpResponse.setHeader(SESSION_ID_HEADER, sessionId);
        httpResponse.setHeader("Cache-Control", "no-cache");
        httpResponse.setHeader("Connection", "keep-alive");
        httpResponse.setStatus(200);

        PrintWriter writer = httpResponse.getWriter();
        writer.write("data: " + objectMapper.writeValueAsString(response) + "\n\n");
        writer.write("data: [DONE]\n\n");
        writer.flush();
    }

    private void writeError(HttpServletResponse httpResponse, int status, McpResponse error)
            throws IOException {
        httpResponse.setContentType("application/json");
        httpResponse.setCharacterEncoding("UTF-8");
        httpResponse.setStatus(status);
        httpResponse.getWriter().write(objectMapper.writeValueAsString(error));
        httpResponse.getWriter().flush();
    }

    private boolean isValidAcceptHeader(String accept) {
        if (accept == null) return false;
        return accept.contains("application/json") && accept.contains("text/event-stream");
    }
}
