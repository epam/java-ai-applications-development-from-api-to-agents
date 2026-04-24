package t8.agent.task.agents;

import commons.model.Message;
import t8.agent.task.tools.BaseTool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseAgent {

    protected String model;
    protected String apiKey;
    protected String systemPrompt;
    protected Map<String, BaseTool> toolsDict;

    public BaseAgent(String model, String apiKey, List<BaseTool> tools, String systemPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("API key cannot be null or empty");
        }
        this.model = model;
        this.apiKey = apiKey;
        this.systemPrompt = systemPrompt;
        this.toolsDict = new HashMap<>();
        if (tools != null) {
            for (BaseTool tool : tools) {
                toolsDict.put(tool.getName(), tool);
            }
        }
    }

    /**
     * Send the conversation to the LLM and return its reply.
     * Tool calls are handled transparently via recursion until a plain text response is returned.
     * The messages list is mutated in-place to accumulate intermediate tool-call and tool-result messages.
     */
    public abstract Message getResponse(List<Message> messages, boolean printRequest);

    protected String callTool(String functionName, Map<String, Object> arguments) {
        BaseTool tool = toolsDict.get(functionName);
        if (tool != null) {
            return tool.execute(arguments);
        }
        return "Unknown function: " + functionName;
    }
}
