package t1.llm.api.anthropic;

import t1.llm.api.BaseApp;

import static commons.Constants.*;

public class AnthropicApp {

    public static void main(String[] args) {
        var sdkClient = new AnthropicAiClient(
                ANTHROPIC_ENDPOINT,
                CLAUDE_SONNET_4_5,
                ANTHROPIC_API_KEY,
                DEFAULT_SYSTEM_PROMPT
        );
        var customClient = new CustomAnthropicAiClient(
                ANTHROPIC_ENDPOINT,
                CLAUDE_SONNET_4_5,
                ANTHROPIC_API_KEY,
                DEFAULT_SYSTEM_PROMPT
        );

        // Switch between sdkClient and customClient to compare SDK vs raw HTTP
        BaseApp.start(true, sdkClient);
    }
}
