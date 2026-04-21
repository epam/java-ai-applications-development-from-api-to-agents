package t2.llms.output.tuning;

import t2.llms.output.tuning.clients.AnthropicAiClient;

import java.util.Map;

import static commons.Constants.CLAUDE_SONNET_4_5;

public class AnthropicTask {

    public static void main(String[] args) {
        TuningApp.run(
                new AnthropicAiClient(CLAUDE_SONNET_4_5),
                true,
                false,
                Map.of("max_tokens", 16)
        );
    }
}
