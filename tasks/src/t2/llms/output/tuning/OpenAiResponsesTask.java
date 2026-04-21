package t2.llms.output.tuning;

import t2.llms.output.tuning.clients.OpenAiResponsesClient;

import java.util.Map;

import static commons.Constants.GPT_5_4;

public class OpenAiResponsesTask {

    public static void main(String[] args) {
        TuningApp.run(
                new OpenAiResponsesClient(GPT_5_4),
                true,
                false,
                Map.of("max_output_tokens", 16)
        );
    }
}
