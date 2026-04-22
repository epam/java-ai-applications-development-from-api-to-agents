package t3.content.generation.t1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import commons.Constants;
import commons.exceptions.TaskNotImplementedException;

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
        //TODO:
        // https://platform.openai.com/docs/guides/vision
        // https://developers.openai.com/api/docs/guides/images-vision?format=url&lang=curl
        // https://developers.openai.com/api/docs/guides/images-vision?format=base64-encoded
        // 1. Resolve the path to 'logo.png' and read its bytes using Files.readAllBytes(path):
        //    Path logoPath = Path.of("tasks/src/t3/content/generation/t1/logo.png");
        //    byte[] logoBytes = Files.readAllBytes(logoPath);
        // 2. Encode bytes to base64 and prepend the data URL prefix:
        //    String dataUrl = "data:image/png;base64," + Base64.getEncoder().encodeToString(logoBytes);
        // 3. Prepare a JSON template for /v1/chat/completions with placeholders for model and image URLs:
        //    String jsonTemplate = "{\"model\": \"%s\", \"messages\": [...]}";
        // 4. Use String.format() to inject Constants.GPT_5_4, a remote URL, and your dataUrl into the template:
        //    String remoteUrl = "https://a-z-animals.com/media/2019/11/Elephant-male-1024x535.jpg";
        //    String jsonString = String.format(jsonTemplate, Constants.GPT_5_4, remoteUrl, dataUrl);
        // 5. Build and send the HttpRequest using Constants.OPENAI_CHAT_COMPLETIONS_ENDPOINT:
        //    HttpRequest request = HttpRequest.newBuilder().uri(...).POST(...).build();
        //    HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        // 6. Parse the response JSON and extract the poem from path choices/0/message/content:
        //    JsonNode responseJson = MAPPER.readTree(response.body());
        //    String poem = responseJson.path("choices").path(0).path("message").path("content").asText();
        
        // 7. Print the resulting poem.
        throw new TaskNotImplementedException();
    }
}
