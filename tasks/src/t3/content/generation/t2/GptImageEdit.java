package t3.content.generation.t2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import commons.Constants;

import java.io.ByteArrayOutputStream;
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
 * T3-2c: GPT-Image-1 Image Edit
 * <p>
 * Edits an existing image via /v1/images/edits using gpt-image-1.
 * Request is multipart/form-data (NOT JSON) and includes the original image,
 * the model name, and an edit prompt. The response returns the edited image
 * as base64 JSON (b64_json) which is decoded and saved as a PNG file.
 */
public class GptImageEdit {

    private static final String OPENAI_IMAGES_EDITS_ENDPOINT = "https://api.openai.com/v1/images/edits";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        String model = "gpt-image-2";
        String prompt = "Transform this DIALX Community logo by adding magical sparkles, "
                + "glowing stars, and a soft mystical aura around the letters. "
                + "Keep the original text and shape clearly readable.";

        // Resolve input image path (logo.png lives in t1)
        Path imagePath = Path.of("tasks/src/t3/content/generation/t2/logo.png");
        if (!Files.exists(imagePath)) {
            imagePath = Path.of("src/t3/content/generation/t2/logo.png");
        }

        byte[] imageBytes = Files.readAllBytes(imagePath);

        String boundary = "----JavaFormBoundary" + System.currentTimeMillis();

        // Build multipart/form-data body manually
        ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();

        // Model field
        String modelPart = String.format("--%s\r\nContent-Disposition: form-data; name=\"model\"\r\n\r\n%s\r\n",
                boundary, model);
        bodyStream.write(modelPart.getBytes());

        // Prompt field
        String promptPart = String.format("--%s\r\nContent-Disposition: form-data; name=\"prompt\"\r\n\r\n%s\r\n",
                boundary, prompt);
        bodyStream.write(promptPart.getBytes());

        // Image field header
        String fileHeader = String.format("--%s\r\nContent-Disposition: form-data; name=\"image\"; filename=\"%s\"\r\nContent-Type: image/png\r\n\r\n",
                boundary, imagePath.getFileName().toString());
        bodyStream.write(fileHeader.getBytes());

        // Image content
        bodyStream.write(imageBytes);

        // End boundary
        bodyStream.write((String.format("\r\n--%s--\r\n", boundary)).getBytes());

        System.out.println("REQUEST:");
        System.out.println("  url: " + OPENAI_IMAGES_EDITS_ENDPOINT);
        System.out.println("  model: " + model);
        System.out.println("  prompt: " + prompt);
        System.out.println("  image: " + imagePath.getFileName());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_IMAGES_EDITS_ENDPOINT))
                .header("Authorization", "Bearer " + Constants.OPENAI_API_KEY)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(bodyStream.toByteArray()))
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonNode responseJson = MAPPER.readTree(response.body());

        // Decode the base64 image data
        String imageBase64 = responseJson.path("data").path(0).path("b64_json").asText();
        byte[] editedBytes = Base64.getDecoder().decode(imageBase64);

        // Determine the output directory (t2 folder)
        Path outputDir = Path.of("tasks/src/t3/content/generation/t2/");
        if (!Files.exists(outputDir)) {
            outputDir = Path.of("src/t3/content/generation/t2/");
        }

        Files.createDirectories(outputDir);

        String filename = "edited_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".png";
        Path outputPath = outputDir.resolve(filename);

        Files.write(outputPath, editedBytes);
        System.out.println("Edited image saved as " + outputPath.toAbsolutePath());
    }
}