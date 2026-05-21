package t1.llm.api;


import commons.exceptions.TaskNotImplementedException;
import commons.model.Message;

import java.util.List;

/**
 * Abstract base class for AI service clients.
 * <p>
 * Defines the interface all provider implementations must follow. Handles common
 * initialization and API key validation. Both response methods are synchronous — streaming
 * prints tokens to stdout incrementally before returning the complete message.
 */
public abstract class AiClient {

    protected String apiKey;
    protected String endpoint;
    protected String modelName;
    protected String systemPrompt;

    protected AiClient(String endpoint, String modelName, String apiKey, String systemPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("API KEY must not be null or empty");
        }
        this.endpoint = endpoint;
        this.modelName = modelName;
        this.apiKey = apiKey;
        this.systemPrompt = systemPrompt;
    }

    /**
     * Send a request to the AI API and return the complete response at once.
     *
     * @param messages conversation history to send
     * @return the AI's response as a Message with role ASSISTANT
     */
    public abstract Message response(List<Message> messages);

    /**
     * Send a streaming request to the AI API, printing each token to stdout as it arrives,
     * then return the assembled complete message.
     *
     * @param messages conversation history to send
     * @return the complete AI response after all tokens have been received
     */
    public abstract Message streamResponse(List<Message> messages);
}
