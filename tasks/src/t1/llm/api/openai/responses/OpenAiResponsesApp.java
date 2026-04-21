package t1.llm.api.openai.responses;

import t1.llm.api.BaseApp;

import static commons.Constants.DEFAULT_SYSTEM_PROMPT;
import static commons.Constants.GPT_5_4;
import static commons.Constants.OPENAI_API_KEY;
import static commons.Constants.OPENAI_RESPONSES_ENDPOINT;


public class OpenAiResponsesApp {

    public static void main(String[] args) {
        var sdkClient = new OpenAiResponsesClient(
                OPENAI_RESPONSES_ENDPOINT,
                GPT_5_4,
                OPENAI_API_KEY,
                DEFAULT_SYSTEM_PROMPT
        );
        var customClient = new CustomOpenAiResponsesClient(
                OPENAI_RESPONSES_ENDPOINT,
                GPT_5_4,
                OPENAI_API_KEY,
                DEFAULT_SYSTEM_PROMPT
        );

        // Switch between sdkClient and customClient to compare SDK vs raw HTTP
        BaseApp.start(true, sdkClient);
    }
}
