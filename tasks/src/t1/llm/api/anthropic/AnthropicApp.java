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

        // Switch between sdkClient/customClient and stream=true/false to compare combinations
        boolean stream = true;
        String clientName = "CustomAnthropicAiClient (HTTP)"; // "AnthropicAiClient (SDK)"; // or "CustomAnthropicAiClient (HTTP)";
        var client = sdkClient; // or sdkClient

        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("  Client : " + clientName);
        System.out.println("  Stream : " + stream);
        System.out.println("╚══════════════════════════════════════════════════╝");

        BaseApp.start(stream, client);
    }
}
