package t10.mcp.advanced.agent.clients;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Custom MCP client — raw HTTP implementation without external MCP libraries.
 * Uses string-based SSE parsing instead of Java Streams, making the protocol
 * handling explicit and easy to follow step by step.
 */
public class CustomMcpClient extends BaseMcpClient {

    private static final String SESSION_ID_HEADER = "Mcp-Session-Id";

    private final java.net.http.HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private String sessionId;

    private CustomMcpClient(String serverUrl) {
        super(serverUrl);
        this.httpClient = java.net.http.HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public static CustomMcpClient create(String serverUrl) {
        CustomMcpClient client = new CustomMcpClient(serverUrl);
        client.connect();
        return client;
    }

    private void connect() {
        try {
            Map<String, Object> initParams = new LinkedHashMap<>();
            initParams.put("protocolVersion", "2024-11-05");
            initParams.put("capabilities", Map.of("tools", Map.of()));
            initParams.put("clientInfo", Map.of("name", "my-custom-mcp-client", "version", "1.0.0"));

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

        // Use ofString to receive the full body, then parse SSE lines manually
        HttpResponse<String> response = httpClient.send(
                builder.build(), HttpResponse.BodyHandlers.ofString());

        // Extract session ID from response headers
        response.headers().firstValue(SESSION_ID_HEADER).ifPresent(id -> {
            this.sessionId = id;
            System.out.println("Session ID: " + id);
        });

        if (response.statusCode() == 202) {
            return Map.of();
        }

        String json = parseSseBody(response.body());

        Map<String, Object> parsed = objectMapper.readValue(json, Map.class);

        if (parsed.containsKey("error")) {
            Map<String, Object> error = (Map<String, Object>) parsed.get("error");
            throw new RuntimeException("MCP Error " + error.get("code") + ": " + error.get("message"));
        }

        return parsed;
    }

    private String parseSseBody(String body) {
        // Parse SSE line by line: look for "data: " prefix, skip "[DONE]"
        for (String line : body.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("data: ")) {
                String data = trimmed.substring(6).trim();
                if (!data.equals("[DONE]") && !data.isEmpty()) {
                    return data;
                }
            }
        }
        throw new RuntimeException("No valid JSON data found in SSE response");
    }

    private void sendNotification(String method) throws IOException, InterruptedException {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("jsonrpc", "2.0");
        requestBody.put("method", method);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)));

        if (sessionId != null) {
            builder.header(SESSION_ID_HEADER, sessionId);
        }

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        // Refresh session ID if server sends it back
        response.headers().firstValue(SESSION_ID_HEADER).ifPresent(id -> this.sessionId = id);
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
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("name", name);
            params.put("arguments", arguments);

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
