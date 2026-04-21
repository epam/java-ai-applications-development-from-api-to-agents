package t2.llms.output.tuning;

import t2.llms.output.tuning.clients.OpenAiChatCompletionsClient;

import java.util.Map;

import static commons.Constants.GPT_5_4;

public class OpenAiChatCompletionsTask {

    public static void main(String[] args) {
        TuningApp.run(
                new OpenAiChatCompletionsClient(GPT_5_4),
                true,
                false,
                Map.of("max_completion_tokens", 16)
        );
    }
}
