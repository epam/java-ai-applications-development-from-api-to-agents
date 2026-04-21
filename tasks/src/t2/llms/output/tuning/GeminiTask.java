package t2.llms.output.tuning;

import t2.llms.output.tuning.clients.GeminiAiClient;

import java.util.LinkedHashMap;
import java.util.Map;

import static commons.Constants.GEMINI_3_FLASH_PREVIEW;

public class GeminiTask {

    public static void main(String[] args) {
        TuningApp.run(
                new GeminiAiClient(GEMINI_3_FLASH_PREVIEW),
                true,
                false,
                Map.of(
                        "generationConfig",
                        new LinkedHashMap<>(Map.of(
                                "maxOutputTokens", 16,
                                "thinkingConfig", new LinkedHashMap<>(Map.of("includeThoughts", true))
                        ))
                )
        );
    }
}
