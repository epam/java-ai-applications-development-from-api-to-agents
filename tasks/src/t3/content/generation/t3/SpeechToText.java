package t3.content.generation.t3;

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

/**
 * T3-3: Speech to Text (Transcription)
 * <p>
 * Transcribes audio_sample.mp3 via /v1/audio/transcriptions using multipart/form-data.
 * Try both WHISPER_1 and GPT_4O_TRANSCRIBE models and compare the results.
 */
public class SpeechToText {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        // Resolve input path
        Path audioPath = Path.of("tasks/src/t3/content/generation/t3/audio_sample.mp3");
        if (!Files.exists(audioPath)) {
            audioPath = Path.of("src/t3/content/generation/t3/audio_sample.mp3");
        }
        
        byte[] audioBytes = Files.readAllBytes(audioPath);

        String boundary = "----JavaFormBoundary" + System.currentTimeMillis();
        String model = Constants.GPT_4O_TRANSCRIBE;

        // Build multipart/form-data body manually
        ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();
        
        // Model field using a string template for consistency
        String modelPart = String.format("--%s\r\nContent-Disposition: form-data; name=\"model\"\r\n\r\n%s\r\n", 
                boundary, model);
        bodyStream.write(modelPart.getBytes());

        // File field header
        String fileHeader = String.format("--%s\r\nContent-Disposition: form-data; name=\"file\"; filename=\"audio_sample.mp3\"\r\nContent-Type: audio/mpeg\r\n\r\n", 
                boundary);
        bodyStream.write(fileHeader.getBytes());
        
        // File content
        bodyStream.write(audioBytes);
        
        // End boundary
        bodyStream.write((String.format("\r\n--%s--\r\n", boundary)).getBytes());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Constants.OPENAI_AUDIO_TRANSCRIPTIONS_ENDPOINT))
                .header("Authorization", "Bearer " + Constants.OPENAI_API_KEY)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(bodyStream.toByteArray()))
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonNode responseJson = MAPPER.readTree(response.body());
        System.out.println("RESPONSE:\n" + MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(responseJson));

        String transcription = responseJson.path("text").asText();
        System.out.println("\nTranscription:\n" + transcription);

        // Determine the output directory (t5 folder where SpeechToSpeech class is)
        Path outputDir = Path.of("tasks/src/t3/content/generation/t5/");
        if (!Files.exists(outputDir)) {
            outputDir = Path.of("src/t3/content/generation/t5/");
        }
        
        // Ensure the directory exists
        Files.createDirectories(outputDir);

        String filename = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".txt";
        Path outputPath = outputDir.resolve(filename);
        
        Files.writeString(outputPath, transcription);
        System.out.println("Transcription saved to " + outputPath.toAbsolutePath());
    }
}
