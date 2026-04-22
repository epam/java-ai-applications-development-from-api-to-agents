package t3.content.generation.t5;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
 * T3-5: Speech to Speech
 * <p>
 * Sends an audio question (question.mp3) as a base64-encoded input_audio message
 * to gpt-4o-audio-preview via /v1/chat/completions with modalities=["text","audio"].
 * The model responds with both text and audio; the audio is decoded from base64
 * and saved as an MP3 file.
 */
public class SpeechToSpeech {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        // Encode the input audio file to base64
        byte[] audioBytes = Files.readAllBytes(Path.of("tasks/src/t3/content/generation/t5/question.mp3"));
        String audioBase64 = Base64.getEncoder().encodeToString(audioBytes);

        // Static config loaded from a JSON template
        ObjectNode body = (ObjectNode) MAPPER.readTree("""
                {
                    "model": "gpt-4o-audio-preview",
                    "modalities": ["text", "audio"],
                    "audio": {"voice": "ballad", "format": "mp3"}
                }
                """);

        // Add the messages array with the base64-encoded audio input
        var userMessage = body.putArray("messages").addObject();
        userMessage.put("role", "user");
        userMessage.putArray("content")
                .addObject()
                .put("type", "input_audio")
                .putObject("input_audio")
                .put("data", audioBase64)
                .put("format", "mp3");

        System.out.println("REQUEST (audio data truncated for display):\n" + truncatedRequestLog(body));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Constants.OPENAI_CHAT_COMPLETIONS_ENDPOINT))
                .header("Authorization", "Bearer " + Constants.OPENAI_API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        }

        var json = MAPPER.readTree(response.body());

        // Print the text part of the response
        String textContent = json.path("choices").path(0).path("message").path("content").asText("");
        System.out.println("Text response: " + textContent);

        // Decode and save the audio part of the response
        String audioData = json.path("choices").path(0).path("message").path("audio").path("data").asText();
        if (!audioData.isBlank()) {
            byte[] responseAudio = Base64.getDecoder().decode(audioData);

            Path outputDir = Path.of("tasks/src/t3/content/generation/t5/");

            String filename = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".mp3";
            Path outputPath = outputDir.resolve(filename);

            Files.write(outputPath, responseAudio);
            System.out.println("Audio response saved to " + filename);
        }
    }

    private static String truncatedRequestLog(ObjectNode body) throws Exception {
        // Deep-copy and truncate base64 audio data for readable logging
        ObjectNode copy = body.deepCopy();
        try {
            ((ObjectNode) copy.path("messages").path(0)
                    .path("content").path(0)
                    .path("input_audio"))
                    .put("data", "<base64-encoded audio>");
        } catch (Exception ignored) {
        }
        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(copy);
    }
}
