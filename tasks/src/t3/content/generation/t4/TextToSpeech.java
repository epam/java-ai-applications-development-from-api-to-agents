package t3.content.generation.t4;

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

/**
 * T3-4: Text to Speech
 * <p>
 * Converts text to speech via /v1/audio/speech using gpt-4o-mini-tts.
 * The response is raw binary audio — saved directly as an MP3 file.
 * Try different voices from the Voice enum and the instructions field
 * to control speaking style.
 */
public class TextToSpeech {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    public enum Voice {
        ALLOY("alloy"),
        ASH("ash"),
        BALLAD("ballad"),
        CORAL("coral"),
        ECHO("echo"),
        FABLE("fable"),
        NOVA("nova"),
        ONYX("onyx"),
        SAGE("sage"),
        SHIMMER("shimmer");

        private final String value;

        Voice(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static void main(String[] args) throws Exception {
        //TODO:
        // https://developers.openai.com/api/reference/resources/audio/subresources/speech/methods/create
        // 1. Create an ObjectNode for the request body and set "model", "input", "voice", and "instructions" fields:
        //    ObjectNode body = MAPPER.createObjectNode();
        //    body.put("model", "gpt-4o-mini-tts");
        //    body.put("input", "Why can't we say that black is white?");
        //    body.put("voice", Voice.CORAL.getValue());
        //    body.put("instructions", "Speak in a cheerful and positive tone.");
        // 2. Build and send the HttpRequest to Constants.OPENAI_AUDIO_SPEECH_ENDPOINT:
        //    HttpRequest request = HttpRequest.newBuilder().uri(...).POST(...).build();
        //    Note: The TTS endpoint returns raw binary audio, not JSON. Use BodyHandlers.ofByteArray().
        //    HttpResponse<byte[]> response = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
        // 3. Create a filename using LocalDateTime and resolve the output directory (tasks/src/t3/content/generation/t4/):
        //    String filename = LocalDateTime.now().format(...) + ".mp3";
        //    Path outputPath = Path.of("tasks/src/t3/content/generation/t4/").resolve(filename);
        // 4. Save the response body (byte array) to the file:
        //    Files.write(outputPath, response.body());
        // 5. Verify the file was saved and try listening to it.
        throw new commons.exceptions.TaskNotImplementedException();
    }
}
