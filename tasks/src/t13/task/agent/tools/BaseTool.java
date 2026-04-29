package t13.task.agent.tools;

import t13.task.agent.Message;

import java.util.Map;

public abstract class BaseTool {

    public Message execute(String toolCallId, Map<String, Object> arguments) {
        String content;
        try {
            content = executeInternal(arguments);
        } catch (Exception e) {
            content = "ERROR during tool call execution:\n " + e;
        }
        return new Message("tool", content, toolCallId, null);
    }

    protected abstract String executeInternal(Map<String, Object> arguments) throws Exception;

    public abstract String getName();

    public abstract String getDescription();

    public abstract Map<String, Object> getParameters();

    public Map<String, Object> getSchema() {
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", getName(),
                        "description", getDescription(),
                        "parameters", getParameters()
                )
        );
    }
}
