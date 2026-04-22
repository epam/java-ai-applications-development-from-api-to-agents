package t3.content.generation.t2;

import com.fasterxml.jackson.databind.ObjectMapper;
import commons.Constants;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * T3-2a: DALL-E 3 Image Generation
 * <p>
 * Generates an image from a text prompt via /v1/images/generations.
 * The response contains a URL to the generated image.
 * Experiment with Size, Style, and Quality to see the differences.
 */
public class DalleImageGeneration {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    /**
     * The size of the generated image.
     */
    public enum Size {
        SQUARE("1024x1024"),
        PORTRAIT("1024x1792"),
        LANDSCAPE("1792x1024");

        private final String value;

        Size(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * vivid — hyper-real and dramatic; natural — less hyper-real, more realistic.
     */
    public enum Style {
        NATURAL("natural"),
        VIVID("vivid");

        private final String value;

        Style(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * hd creates images with finer details and greater consistency.
     */
    public enum Quality {
        STANDARD("standard"),
        HD("hd");

        private final String value;

        Quality(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static void main(String[] args) throws Exception {
        String body = """
                {
                    "model": "dall-e-3",
                    "prompt": "Smiling catdog",
                    "size": "1024x1024",
                    "style": "natural",
                    "quality": "hd"
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

        var json = MAPPER.readTree(response.body());
        System.out.println("RESPONSE:\n" + MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(json));

        String imageUrl = json.path("data").path(0).path("url").asText();
        System.out.println("\nGenerated image URL:\n" + imageUrl);
    }
}
