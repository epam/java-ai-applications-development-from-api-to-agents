package t2.llms.output.tuning.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import commons.model.Message;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class AIClient {

    protected static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    protected final String endpoint;
    protected final String modelName;
    protected final String apiKey;
    protected final String apiKeyHeaderName;

    protected AIClient(String endpoint, String modelName, String apiKey, String apiKeyHeaderName) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("API key cannot be null or empty");
        }

        this.endpoint = endpoint;
        this.modelName = modelName;
        this.apiKey = apiKey;
        this.apiKeyHeaderName = apiKeyHeaderName;
    }

    public abstract Message response(
            List<Message> messages,
            boolean printRequest,
            boolean printOnlyContent,
            Map<String, Object> options
    );

    protected void printRequest(Map<String, Object> requestData, Map<String, String> headers) {
        System.out.println("\n" + "=".repeat(50) + " REQUEST " + "=".repeat(50));
        System.out.println("Endpoint: " + endpoint);

        System.out.println("\nHeaders:");
        safeHeaders(headers).forEach((key, value) -> System.out.printf("  %s: %s%n", key, value));

        System.out.println("\nRequest Body:");
        System.out.println(toPrettyJson(requestData));
    }

    protected void printResponse(Object responseData, boolean printOnlyContent, String content) {
        System.out.println("=".repeat(50) + " RESPONSE " + "=".repeat(50));
        if (printOnlyContent) {
            System.out.println(content);
        } else {
            System.out.println(toPrettyJson(responseData));
        }
        System.out.println("=".repeat(109));
    }

    protected Map<String, String> safeHeaders(Map<String, String> headers) {
        var safeHeaders = new LinkedHashMap<>(headers);
        var value = safeHeaders.get(apiKeyHeaderName);
        if (value != null) {
            safeHeaders.put(apiKeyHeaderName, maskSecret(value));
        }
        return safeHeaders;
    }

    protected static Map<String, Object> deepCopy(Map<String, Object> source) {
        return MAPPER.convertValue(source, new TypeReference<>() {});
    }

    protected static String post(String url, Map<String, String> headers, Map<String, Object> body) {
        try {
            var request = HttpRequest.newBuilder(URI.create(url));
            headers.forEach(request::header);
            request.POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)));

            var response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return response.body();
            }
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Request interrupted", e);
        } catch (IOException e) {
            throw new RuntimeException("Request failed", e);
        }
    }

    protected static Map<String, Object> castMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            var cast = (Map<String, Object>) map;
            return cast;
        }
        throw new IllegalArgumentException("Expected a map but got: " + value);
    }

    private static String toPrettyJson(Object value) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize JSON", e);
        }
    }

    private static String maskSecret(String secret) {
        if (secret.length() <= 12) {
            return "***";
        }
        return secret.substring(0, 8) + "..." + secret.substring(secret.length() - 4);
    }
}
