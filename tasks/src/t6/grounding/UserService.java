package t6.grounding;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import commons.Constants;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public class UserService {

    private static final String BASE_URL = Constants.USER_SERVICE_ENDPOINT;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = buildObjectMapper();

    private static ObjectMapper buildObjectMapper() {
        // The API may return complex JSON objects for fields declared as String (e.g. address, credit_card).
        // This deserializer converts any JSON token (object, array, primitive, null) to a String.
        SimpleModule module = new SimpleModule();
        module.addDeserializer(String.class, new StdDeserializer<>(String.class) {
            @Override
            public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                if (p.currentToken() == JsonToken.START_OBJECT || p.currentToken() == JsonToken.START_ARRAY) {
                    return p.readValueAsTree().toString();
                }
                return p.getValueAsString();
            }
        });
        return new ObjectMapper().registerModule(module);
    }

    public List<User> getAllUsers() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/v1/users"))
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                List<User> users = objectMapper.readValue(response.body(), new TypeReference<>() {});
                System.out.println("Get " + users.size() + " users successfully");
                return users;
            }
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<User> getUser(int id) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/v1/users/" + id))
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return Optional.of(objectMapper.readValue(response.body(), User.class));
            }
            if (response.statusCode() == 404) {
                System.out.println("User with ID " + id + " is absent (404)");
                return Optional.empty();
            }
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public List<User> searchUsers(String name, String surname, String email) {
        try {
            StringBuilder params = new StringBuilder();
            if (name != null) appendParam(params, "name", name);
            if (surname != null) appendParam(params, "surname", surname);
            if (email != null) appendParam(params, "email", email);

            String url = BASE_URL + "/v1/users/search" +
                         (params.isEmpty() ? "" : "?" + params);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                List<User> users = objectMapper.readValue(response.body(), new TypeReference<>() {});
                System.out.println("Get " + users.size() + " users successfully");
                return users;
            }
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void appendParam(StringBuilder sb, String key, String value) {
        if (!sb.isEmpty()) sb.append("&");
        sb.append(key).append("=").append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }
}
