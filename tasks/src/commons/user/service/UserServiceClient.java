package commons.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public class UserServiceClient {

    private static final String USER_SERVICE_ENDPOINT = "http://localhost:8041";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public UserServiceClient() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    private String userToString(Map<String, Object> user) {
        StringBuilder sb = new StringBuilder("```\n");
        user.forEach((key, value) -> sb.append("  ").append(key).append(": ").append(value).append("\n"));
        sb.append("```\n");
        return sb.toString();
    }

    private String usersToString(List<Map<String, Object>> users) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> user : users) {
            sb.append(userToString(user));
        }
        sb.append("\n");
        return sb.toString();
    }

    public String getUser(int userId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(USER_SERVICE_ENDPOINT + "/v1/users/" + userId))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                Map<String, Object> data = objectMapper.readValue(response.body(), Map.class);
                return userToString(data);
            }
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public String searchUsers(
            String name,
            String surname,
            String email,
            String gender
    ) {
        StringBuilder uriBuilder = new StringBuilder(USER_SERVICE_ENDPOINT).append("/v1/users/search?");
        if (name != null) uriBuilder.append("name=").append(name).append("&");
        if (surname != null) uriBuilder.append("surname=").append(surname).append("&");
        if (email != null) uriBuilder.append("email=").append(email).append("&");
        if (gender != null) uriBuilder.append("gender=").append(gender).append("&");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uriBuilder.toString()))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                List<Map<String, Object>> data = objectMapper.readValue(response.body(), List.class);
                System.out.println("Get " + data.size() + " users successfully");
                return usersToString(data);
            }
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public String addUser(UserCreate userCreateModel) {
        try {
            String json = objectMapper.writeValueAsString(userCreateModel);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(USER_SERVICE_ENDPOINT + "/v1/users"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 201) {
                return "User successfully added: " + response.body();
            }
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public String updateUser(int userId, UserUpdate userUpdateModel) {
        try {
            String json = objectMapper.writeValueAsString(userUpdateModel);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(USER_SERVICE_ENDPOINT + "/v1/users/" + userId))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return "User successfully updated: " + response.body();
            }
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public String deleteUser(int userId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(USER_SERVICE_ENDPOINT + "/v1/users/" + userId))
                .header("Content-Type", "application/json")
                .DELETE()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 204) {
                return "User successfully deleted";
            }
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
