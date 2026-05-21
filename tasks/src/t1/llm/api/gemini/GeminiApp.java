package t1.llm.api.gemini;

import t1.llm.api.BaseApp;

import static commons.Constants.DEFAULT_SYSTEM_PROMPT;
import static commons.Constants.GEMINI_3_FLASH_PREVIEW;
import static commons.Constants.GEMINI_API_KEY;
import static commons.Constants.GEMINI_ENDPOINT;


public class GeminiApp {

    public static void main(String[] args) {
        var sdkClient = new GeminiAiClient(
                GEMINI_ENDPOINT,
                GEMINI_3_FLASH_PREVIEW,
                GEMINI_API_KEY,
                DEFAULT_SYSTEM_PROMPT
        );
        var customClient = new CustomGeminiAiClient(
                GEMINI_ENDPOINT,
                GEMINI_3_FLASH_PREVIEW,
                GEMINI_API_KEY,
                DEFAULT_SYSTEM_PROMPT
        );

        // Switch between sdkClient/customClient and stream=true/false to compare combinations
        boolean stream = true;
        String clientName = "CustomGeminiAiClient (HTTP)"; // or "GeminiAiClient (SDK)" // "CustomGeminiAiClient (HTTP)"
        var client = customClient; // or sdkClient

        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("  Client : " + clientName);
        System.out.println("  Stream : " + stream);
        System.out.println("╚══════════════════════════════════════════════════╝");

        BaseApp.start(stream, client);
    }
}
