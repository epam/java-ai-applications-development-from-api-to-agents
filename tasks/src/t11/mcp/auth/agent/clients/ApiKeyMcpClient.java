package t11.mcp.auth.agent.clients;

import java.net.http.HttpRequest;
import java.util.function.Consumer;

/**
 * MCP client that authenticates via a static API key in the {@code X-API-Key} header.
 *
 * <p>The key is injected into every request (including the initial {@code initialize} handshake),
 * so the server can validate it before any MCP traffic is processed.
 */
public class ApiKeyMcpClient extends BaseMcpClient {

    private final String apiKey;

    public ApiKeyMcpClient(String mcpServerUrl, String apiKey) {
        super(mcpServerUrl);
        this.apiKey = apiKey;
    }

    @Override
    protected Consumer<HttpRequest.Builder> requestCustomizer() {
        return rb -> rb.header("X-API-Key", apiKey);
    }
}
