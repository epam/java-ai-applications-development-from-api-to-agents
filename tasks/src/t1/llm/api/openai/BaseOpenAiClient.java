package t1.llm.api.openai;

import commons.exceptions.TaskNotImplementedException;
import t1.llm.api.AiClient;

/**
 * Abstract base for OpenAI clients.
 * <p>
 * Validates the raw API key and prepends "Bearer " before storing it, matching
 * the Authorization header format required by the OpenAI REST API.
 */
public abstract class BaseOpenAiClient extends AiClient {

    protected BaseOpenAiClient(String endpoint, String modelName, String apiKey, String systemPrompt) {
        super(endpoint, modelName, withBearer(apiKey), systemPrompt);
    }

    private static String withBearer(String apiKey) {
        //TODO:
        // - Validate that apiKey is not null or blank; throw IllegalArgumentException if invalid
        // - Return the apiKey with "Bearer " prepended (note the trailing space before the key)

        if (isEmpty(apiKey)) {
            throw new IllegalArgumentException("API key cannot be empty");
        }

        return "Bearer ".concat(apiKey);
    }

    private static boolean isEmpty(String string) {
        return string == null || string.isEmpty();
    }
}
