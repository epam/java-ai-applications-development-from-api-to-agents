package t2.llms.output.tuning;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import t2.llms.output.tuning.clients.AnthropicAiClient;

import java.util.Map;

import static commons.Constants.CLAUDE_SONNET_4_5;

public class AnthropicTask {

    public static void main(String[] args) throws JsonProcessingException {
        TuningApp.run(
                new AnthropicAiClient(CLAUDE_SONNET_4_5),
                true,
                false,

                // TODO 1: temperature — controls randomness. Range: 0.0-1.0, default: 1.0
                //  Lower = more deterministic, higher = more creative
                //  Query: "Give me a name for a coffee shop"
                //  Try: temperature=0.0 vs temperature=1.0, compare outputs

                // TODO 2: top_p — nucleus sampling, keeps tokens within cumulative probability. Range: 0.0-1.0, default: 1.0 (disabled)
                //  Lower = fewer token choices, more focused output
                //  Query: "List 5 alternative uses for a paperclip"
                //  Try: top_p=0.1 vs top_p=0.9

                // TODO 3: top_k — limits token selection to top K candidates. Default: not set (disabled)
                //  Lower = fewer choices per token, more predictable
                //  Query: "Write a one-sentence story about a robot"
                //  Try: top_k=1 vs top_k=50

                // TODO 4: stop_sequences — list of strings that stop generation when encountered
                //  Query: "Count from 1 to 20, comma separated"
                //  Try: stop_sequences=["10"] — generation stops before reaching 10

                // TODO 5: output_config — enforce structured JSON output
                //  Query: "List 3 programming languages with their year of creation"
                //  Try: "output_config", new ObjectMapper().readTree(
                //        """
                //        {"format":{"type": "json_schema", "schema": {"type": "object", "additionalProperties": false, "properties": {"languages": {"type": "array", "items": {"type": "object", "additionalProperties": false, "properties": {"name": {"type": "string"}, "year": {"type": "integer"}},"required": ["name", "year"]}}}}}}
                //        """)

                // TODO 6: thinking — enables extended thinking (chain-of-thought). Requires budget_tokens param
                //  Model reasons step-by-step before answering. Needs max_tokens > budget_tokens
                //  Query: "How many r's are in the word strawberry?"
                //  Try: "max_tokens", 8000, "thinking", new ObjectMapper().readTree("""{"type": "enabled", "budget_tokens": 5000}""")
                Map.of(

                )
        );
    }
}
