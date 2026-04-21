package t2.llms.output.tuning;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import t2.llms.output.tuning.clients.GeminiAiClient;

import java.util.LinkedHashMap;
import java.util.Map;

import static commons.Constants.GEMINI_3_FLASH_PREVIEW;

public class GeminiTask {

    public static void main(String[] args) throws JsonProcessingException {
        TuningApp.run(
                new GeminiAiClient(GEMINI_3_FLASH_PREVIEW),
                true,
                false,

                // 🚨 All parameters below must be passed inside generationConfig={...} 🚨
                // TODO 1: temperature — controls randomness. Range: 0.0-2.0, default: 1.0
                //  Lower = more deterministic, higher = more creative
                //  Query: "Give me a name for a coffee shop"
                //  Try: "temperature", 0.0 vs "temperature", 2.0, compare outputs

                // TODO 2: topP — nucleus sampling, keeps tokens within cumulative probability. Range: 0.0-1.0, default: 0.95
                //  Lower = fewer token choices, more focused output
                //  Query: "List 5 alternative uses for a paperclip"
                //  Try: "topP", 0.1 vs "topP", 0.95

                // TODO 3: topK — limits token selection to top K candidates. Default: 40
                //  Lower = fewer choices per token, more predictable
                //  Query: "Write a one-sentence story about a robot"
                //  Try: "topK", 1 vs "topK", 64

                // TODO 4: maxOutputTokens — max number of tokens in the response. Required, default: 1024 (set in gemini_client.py)
                //  Query: "Explain quantum computing"
                //  Try: "maxOutputTokens", 50 vs "maxOutputTokens", 2048

                // TODO 5: responseMimeType + responseSchema — enforce structured output format
                //  responseMimeType: "text/plain" (default), "application/json", "text/x.enum"
                //  responseSchema: JSON Schema defining the expected structure (requires responseMimeType="application/json")
                //  Query: "List 3 programming languages with their year of creation"
                //  Try: "responseMimeType", "application/json",
                //       "responseSchema", new ObjectMapper().readTree(
                //       """
                //       {"type": "array", "items": {"type": "object", "properties": {"name": {"type": "string"}, "year": {"type": "integer"}}}}
                //       """)

                // TODO 7: thinkingConfig — enables extended thinking (chain-of-thought). Requires thinkBudget param
                //  Model reasons step-by-step before answering
                //  Query: "How many r's are in the word strawberry?"
                //  Try: "thinkingConfig", new ObjectMapper().readTree("""{"thinkMode": "THINKING_MODE_ENABLED", "thinkBudget": 5000}""")

                Map.of(

                )
        );
    }
}
