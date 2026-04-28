package t12.skills.task.custom.tools;

import java.util.Map;

public abstract class BaseTool {

    public final String execute(Map<String, Object> arguments) {
        try {
            return doExecute(arguments);
        } catch (Exception e) {
            return "ERROR during tool call execution:\n " + e.getMessage();
        }
    }

    protected abstract String doExecute(Map<String, Object> arguments) throws Exception;

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
