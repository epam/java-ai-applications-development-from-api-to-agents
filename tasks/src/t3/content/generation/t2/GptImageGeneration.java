package t3.content.generation.t2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import commons.Constants;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * T3-2b: GPT-Image-1 Image Generation
 * <p>
 * Generates an image via /v1/images/generations using gpt-image-1.
 * Unlike DALL-E 3, the response returns the image as base64 JSON (b64_json)
 * rather than a URL — this implementation decodes and saves it as a PNG file.
 */
public class GptImageGeneration {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        // JSON template for the request body
        String body = """
                {
                    "model": "gpt-image-1",
                    "prompt": "Smiling catdog"
                }
                """;

        System.out.println("REQUEST:\n" + body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Constants.OPENAI_IMAGES_GENERATIONS_ENDPOINT))
                .header("Authorization", "Bearer " + Constants.OPENAI_API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonNode responseJson = MAPPER.readTree(response.body());
        System.out.println("RESPONSE:\n" + MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(responseJson));

        // Decode the base64 image data
        String imageBase64 = responseJson.path("data").path(0).path("b64_json").asText();
        byte[] imageBytes = Base64.getDecoder().decode(imageBase64);

        // Determine the output directory (t3_content_generation/t2/)
        Path outputDir = Path.of("tasks/src/t3/content/generation/t2/");

        String filename = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".png";
        Path outputPath = outputDir.resolve(filename);
        
        Files.write(outputPath, imageBytes);
        System.out.println("Image saved as " + outputPath.toAbsolutePath());
    }
}
