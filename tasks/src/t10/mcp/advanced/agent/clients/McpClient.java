package t10.mcp.advanced.agent.clients;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class McpClient extends BaseMcpClient {

    private static final String SESSION_ID_HEADER = "Mcp-Session-Id";

    private final java.net.http.HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private String sessionId;

    private McpClient(String serverUrl) {
        super(serverUrl);
        this.httpClient = java.net.http.HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public static McpClient create(String serverUrl) {
        McpClient client = new McpClient(serverUrl);
        client.connect();
        return client;
    }

    private void connect() {
        try {
            Map<String, Object> initParams = Map.of(
                    "protocolVersion", "2024-11-05",
                    "capabilities", Map.of("tools", Map.of()),
                    "clientInfo", Map.of("name", "java-mcp-client", "version", "1.0.0")
            );
            Map<String, Object> initResult = sendRequest("initialize", initParams);
            System.out.println(objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(initResult.get("result")));

            sendNotification("notifications/initialized");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to connect to MCP server at " + serverUrl, e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sendRequest(String method, Map<String, Object> params)
            throws IOException, InterruptedException {

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("jsonrpc", "2.0");
        requestBody.put("id", UUID.randomUUID().toString());
        requestBody.put("method", method);
        if (params != null) {
            requestBody.put("params", params);
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)));

        if (!"initialize".equals(method) && sessionId != null) {
            builder.header(SESSION_ID_HEADER, sessionId);
        }

        HttpResponse<Stream<String>> response = httpClient.send(
                builder.build(), HttpResponse.BodyHandlers.ofLines());

        response.headers().firstValue(SESSION_ID_HEADER)
                .ifPresent(id -> this.sessionId = id);

        if (response.statusCode() == 202) {
            return Map.of();
        }

        String json = response.body()
                .filter(line -> line.startsWith("data: "))
                .map(line -> line.substring(6))
                .filter(data -> !data.equals("[DONE]") && !data.isBlank())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No data in SSE response"));

        Map<String, Object> parsed = objectMapper.readValue(json, Map.class);

        if (parsed.containsKey("error")) {
            Map<String, Object> error = (Map<String, Object>) parsed.get("error");
            throw new RuntimeException("MCP Error " + error.get("code") + ": " + error.get("message"));
        }

        return parsed;
    }

    private void sendNotification(String method) throws IOException, InterruptedException {
        Map<String, Object> requestBody = Map.of("jsonrpc", "2.0", "method", method);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)));

        if (sessionId != null) {
            builder.header(SESSION_ID_HEADER, sessionId);
        }

        httpClient.send(builder.build(), HttpResponse.BodyHandlers.discarding());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getTools() {
        try {
            Map<String, Object> response = sendRequest("tools/list", null);
            Map<String, Object> result = (Map<String, Object>) response.get("result");
            List<Map<String, Object>> toolsList = (List<Map<String, Object>>) result.get("tools");

            return toolsList.stream()
                    .map(tool -> Map.of(
                            "type", "function",
                            "function", Map.of(
                                    "name", tool.get("name"),
                                    "description", tool.getOrDefault("description", ""),
                                    "parameters", tool.getOrDefault("inputSchema", Map.of())
                            )
                    ))
                    .collect(Collectors.toList());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to get tools from MCP server", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public String callTool(String name, Map<String, Object> arguments) {
        System.out.println("    Calling `" + name + "` with " + arguments);

        try {
            Map<String, Object> params = Map.of("name", name, "arguments", arguments);
            Map<String, Object> response = sendRequest("tools/call", params);
            Map<String, Object> result = (Map<String, Object>) response.get("result");
            List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");

            if (content != null && !content.isEmpty()) {
                String text = (String) content.get(0).get("text");
                System.out.println("    ⚙️: " + text + "\n");
                return text;
            }
            return "Unexpected error occurred!";
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to call tool '" + name + "'", e);
        }
    }
}
