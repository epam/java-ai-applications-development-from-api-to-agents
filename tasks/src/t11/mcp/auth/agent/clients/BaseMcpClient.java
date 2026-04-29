package t11.mcp.auth.agent.clients;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import t9.mcp.fundamentals.agent.mcp.client.BaseClient;

import java.net.http.HttpRequest;
import java.util.function.Consumer;

/**
 * Abstract base for auth-aware MCP clients.
 *
 * Subclasses provide {@link #requestCustomizer()} to inject auth headers (API key or Bearer token)
 * into every request, and may override {@link #beforeConnect()} to run auth flows (e.g. PKCE).
 */
public abstract class BaseMcpClient extends BaseClient {

    protected final String mcpServerUrl;

    protected BaseMcpClient(String mcpServerUrl) {
        this.mcpServerUrl = mcpServerUrl;
    }

    /**
     * Hook called before the transport is built.
     * Override to run auth flows (e.g. PKCE browser login) before connecting.
     */
    protected void beforeConnect() {}

    @Override
    public final void connect() {
        beforeConnect();
        connectTransport();
    }

    /**
     * Builds the MCP transport with the auth customizer and initializes the client.
     * Called by {@link #connect()} and may be called again by subclasses on token refresh.
     */
    protected final void connectTransport() {
        int lastSlash = mcpServerUrl.lastIndexOf('/');
        String baseUrl = lastSlash > 7 ? mcpServerUrl.substring(0, lastSlash) : mcpServerUrl;
        String endpoint = lastSlash > 7 ? mcpServerUrl.substring(lastSlash) : "/mcp";

        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport
                .builder(baseUrl)
                .endpoint(endpoint)
                .jsonMapper(new JacksonMcpJsonMapper(objectMapper))
                .customizeRequest(requestCustomizer())
                .build();

        mcpClient = McpClient.sync(transport).build();
        System.out.println("Connecting to HTTP MCP Server at " + mcpServerUrl + "...");
        mcpClient.initialize();
        initToolCallbackProvider();
        System.out.println("Connected to HTTP MCP Server.");
    }

    /** Returns a consumer that injects auth headers into each outgoing HTTP request. */
    protected abstract Consumer<HttpRequest.Builder> requestCustomizer();
}
