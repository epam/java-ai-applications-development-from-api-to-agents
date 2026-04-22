package t3.content.generation.t1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import commons.Constants;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * T3-1: Image Analysis (Vision)
 * <p>
 * Sends two images to gpt model via /v1/chat/completions:
 *   - a remote image by URL
 *   - a local logo.png encoded as a base64 data URL
 * and asks the model to write a poem based on both images.
 */
public class ImageAnalysis {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        // Encode the local logo.png to a base64 data URL
        // Using a relative path that works if run from the 'tasks' directory
        Path logoPath = Path.of("src/t3/content/generation/t1/logo.png");
        if (!Files.exists(logoPath)) {
            // Fallback for running from project root
            logoPath = Path.of("tasks/src/t3/content/generation/t1/logo.png");
        }
        
        byte[] logoBytes = Files.readAllBytes(logoPath);
        String dataUrl = "data:image/png;base64," + Base64.getEncoder().encodeToString(logoBytes);
        String remoteUrl = "https://a-z-animals.com/media/2019/11/Elephant-male-1024x535.jpg";

        // JSON template with placeholders for model, remote URL, and local data URL
        // Double curly braces are NOT needed for String.format unless you want literal braces, 
        // but here we are using %s which is fine inside the JSON structure.
        String jsonTemplate = """
                {
                  "model": "%s",
                  "messages": [
                    {
                      "role": "user",
                      "content": [
                        { "type": "text", "text": "Poem based on images" },
                        { "type": "image_url", "image_url": { "url": "%s" } },
                        { "type": "image_url", "image_url": { "url": "%s" } }
                      ]
                    }
                  ]
                }
                """;

        // Use string formatter to inject values
        String jsonString = String.format(jsonTemplate, Constants.GPT_5_4, remoteUrl, dataUrl);
        
        System.out.println("REQUEST:\n" + jsonString);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Constants.OPENAI_CHAT_COMPLETIONS_ENDPOINT))
                .header("Authorization", "Bearer " + Constants.OPENAI_API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonString))
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonNode responseJson = MAPPER.readTree(response.body());
        System.out.println("RESPONSE:\n" + MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(responseJson));

        String poem = responseJson.path("choices").path(0).path("message").path("content").asText();
        System.out.println("\nPoem:\n" + poem);
    }
}
