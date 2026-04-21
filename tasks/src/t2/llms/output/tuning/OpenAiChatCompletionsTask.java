package t2.llms.output.tuning;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import t2.llms.output.tuning.clients.OpenAiChatCompletionsClient;

import java.util.Map;

import static commons.Constants.GPT_5_4;

public class OpenAiChatCompletionsTask {

    public static void main(String[] args) throws JsonProcessingException {
        TuningApp.run(
                new OpenAiChatCompletionsClient(GPT_5_4),
                true,
                false,

                // TODO 1: n — number of completions to generate per request. Default: 1
                //  ⚠️ Note: NOT available in Responses API
                //  Query: "Give me a name for a coffee shop"
                //  Try: n=3 — returns 3 different completions in choices[]

                // TODO 2: temperature — controls randomness. Range: 0.0-2.0, default: 1.0
                //  Lower = more deterministic, higher = more creative
                //  Query: "Why white is white?"
                //  Try: temperature=0.0 vs temperature=2.0, compare outputs
                //  ⚠️ Note: it is okay that after temperature=1.5 you get some odd characters in output 😅

                // TODO 3: top_p — nucleus sampling, keeps tokens within cumulative probability. Range: 0.0-1.0, default: 1.0
                //  Lower = fewer token choices, more focused output
                //  Query: "List 5 alternative uses for a paperclip"
                //  Try: top_p=0.1 vs top_p=0.9

                // TODO 4: max_tokens — max number of tokens in the response. Default: model-dependent
                //  ⚠️ Note: Will work for models like gpt-4o. For gpt-5+ - `max_completion_tokens`.
                //  Query: "Explain quantum computing"
                //  Try: max_tokens=50 vs max_tokens=2048

                // TODO 5: stop — list of strings (up to 4) that stop generation when encountered
                //  ⚠️ Note: Will work for models like gpt-4o
                //  Query: "Count from 1 to 20, comma separated"
                //  Try: stop=["5"] — generation stops before reaching 5

                // TODO 6: response_format — enforce structured output format
                //  "text" (default) or "json_schema" with a schema definition
                //  Query: "List 3 programming languages with their year of creation"
                //  Try: "response_format", new ObjectMapper().readTree(
                //       """
                //       {"type": "json_schema", "json_schema": {"name": "languages", "strict": true, "schema": {"type": "object", "properties": {"languages": {"type": "array", "items": {"type": "object", "properties": {"name": {"type": "string"}, "year": {"type": "integer"}}, "required": ["name", "year"], "additionalProperties": false}}}, "required": ["languages"], "additionalProperties": false}}}
                //       """)

                // TODO 7: frequency_penalty — penalizes tokens based on how often they appeared so far. Range: -2.0 to 2.0, default: 0
                //  ⚠️ Note: Will work for models like gpt-4o
                //  Positive = reduces repetition, negative = encourages repetition
                //  Query: "Write a paragraph about the ocean"
                //  Try: frequency_penalty=0.0 vs frequency_penalty=1.5

                // TODO 8: presence_penalty — penalizes tokens based on whether they appeared at all. Range: -2.0 to 2.0, default: 0
                //  ⚠️ Note: Will work for models like gpt-4o
                //  Positive = encourages new topics, negative = stays on topic
                //  Query: "Write a paragraph about the ocean"
                //  Try: presence_penalty=0.0 vs presence_penalty=1.5

                // TODO 9: seed — attempts deterministic output. Same seed + same input = same output (best effort)
                //  ⚠️ Note: Will work for models like gpt-4o
                //  Query: "Give me a name for a coffee shop"
                //  Try: seed=42 — run twice with the same seed and compare outputs

                // TODO 10: reasoning_effort — controls how much thinking the model does. Values: "low", "medium", "high" (default)
                //  Lower effort = faster, cheaper responses; higher = more thorough reasoning
                //  ⚠️ Note: does NOT work with non-default temperature (must omit temperature or set to 1.0)
                //  Query: "How many r's are in the word strawberry?"
                //  Try: reasoning_effort="low" vs reasoning_effort="high"

                Map.of(
                        "response_format", new ObjectMapper().readTree(
                                """
                                        {"type": "json_schema", "json_schema": {"name": "languages", "strict": true, "schema": {"type": "object", "properties": {"languages": {"type": "array", "items": {"type": "object", "properties": {"name": {"type": "string"}, "year": {"type": "integer"}}, "required": ["name", "year"], "additionalProperties": false}}}, "required": ["languages"], "additionalProperties": false}}}
                                        """
                        )
                )
        );
    }
}
