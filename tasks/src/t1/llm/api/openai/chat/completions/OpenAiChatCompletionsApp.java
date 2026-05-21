package t1.llm.api.openai.chat.completions;

import t1.llm.api.BaseApp;

import static commons.Constants.DEFAULT_SYSTEM_PROMPT;
import static commons.Constants.GPT_5_4;
import static commons.Constants.OPENAI_API_KEY;
import static commons.Constants.OPENAI_CHAT_COMPLETIONS_ENDPOINT;


public class OpenAiChatCompletionsApp {

    public static void main(String[] args) {
        var sdkClient = new OpenAiChatCompletionsClient(
                OPENAI_CHAT_COMPLETIONS_ENDPOINT,
                GPT_5_4,
                OPENAI_API_KEY,
                DEFAULT_SYSTEM_PROMPT
        );
        var customClient = new CustomOpenAiChatCompletionsClient(
                OPENAI_CHAT_COMPLETIONS_ENDPOINT,
                GPT_5_4,
                OPENAI_API_KEY,
                DEFAULT_SYSTEM_PROMPT
        );

        // Switch between sdkClient/customClient and stream=true/false to compare combinations
        boolean stream = false;
        String clientName =  "OpenAiChatCompletionsClient (SDK)"; // or "CustomOpenAiChatCompletionsClient (HTTP)";
        var client = sdkClient; // or sdkClient

        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("  Client : " + clientName);
        System.out.println("  Stream : " + stream);
        System.out.println("╚══════════════════════════════════════════════════╝");

        BaseApp.start(stream, client);
    }

}
