package t13.task.agent.clients;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;

public class HttpMcpClient extends BaseMcpClient {

    private final String mcpServerUrl;

    public HttpMcpClient(String mcpServerUrl) {
        this.mcpServerUrl = mcpServerUrl;
    }

    @Override
    public void connect() {
        String baseUrl;
        String endpoint;

        int lastSlash = mcpServerUrl.lastIndexOf('/');
        if (lastSlash > 7) { // avoid splitting "http://"
            baseUrl = mcpServerUrl.substring(0, lastSlash);
            endpoint = mcpServerUrl.substring(lastSlash);
        } else {
            baseUrl = mcpServerUrl;
            endpoint = "/mcp";
        }

        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport
                .builder(baseUrl)
                .endpoint(endpoint)
                .jsonMapper(new JacksonMcpJsonMapper(objectMapper))
                .build();

        mcpClient = McpClient.sync(transport).build();

        System.out.println("Connecting to HTTP MCP Server at " + mcpServerUrl + "...");
        mcpClient.initialize();
        initToolCallbackProvider();
        System.out.println("Connected to HTTP MCP Server.");
    }
}
