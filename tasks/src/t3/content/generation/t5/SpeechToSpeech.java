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
        //TODO:
        // https://developers.openai.com/api/docs/guides/audio?lang=curl
        // 1. Read 'question.mp3' and encode it to base64:
        //    byte[] audioBytes = Files.readAllBytes(Path.of("tasks/src/t3/content/generation/t5/question.mp3"));
        //    String audioBase64 = Base64.getEncoder().encodeToString(audioBytes);
        // 2. Prepare the request body ObjectNode with "model", "modalities", and "audio" config:
        //    ObjectNode body = MAPPER.createObjectNode();
        //    body.put("model", "gpt-4o-audio-preview");
        //    body.putArray("modalities").add("text").add("audio");
        //    body.putObject("audio").put("voice", "ballad").put("format", "mp3");
        // 3. Add the user message containing the base64-encoded "input_audio":
        //    var userMessage = body.putArray("messages").addObject();
        //    userMessage.put("role", "user");
        //    userMessage.putArray("content").addObject()
        //        .put("type", "input_audio")
        //        .putObject("input_audio").put("data", audioBase64).put("format", "mp3");
        // 4. Build and send the HttpRequest to Constants.OPENAI_CHAT_COMPLETIONS_ENDPOINT:
        //    HttpRequest request = HttpRequest.newBuilder().uri(...).POST(...).build();
        //    HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        // 5. Extract the text response from choices/0/message/content:
        //    String textContent = json.path("choices").path(0).path("message").path("content").asText();
        // 6. Extract the audio data from choices/0/message/audio/data, decode it, and save as a .mp3 file:
        //    String audioData = json.path("choices").path(0).path("message").path("audio").path("data").asText();
        //    byte[] responseAudio = Base64.getDecoder().decode(audioData);
        //    Files.write(Path.of("tasks/src/t3/content/generation/t5/").resolve(filename), responseAudio);
        throw new commons.exceptions.TaskNotImplementedException();
    }

    private static String truncatedRequestLog(ObjectNode body) throws Exception {
        //TODO:
        // 1. Create a deep copy of the body: body.deepCopy()
        // 2. Navigate to messages/0/content/0/input_audio and replace the "data" value with "<base64-encoded audio>" for cleaner logging.
        // 3. Return the pretty-printed JSON string using MAPPER.
        throw new commons.exceptions.TaskNotImplementedException();
    }
}
