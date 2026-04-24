package t8.agent.task.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;

public abstract class BaseTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public abstract String execute(Map<String, Object> arguments);

    public abstract String getName();

    public abstract String getDescription();

    public abstract String getInputSchema();

    public String getOpenAiSchema() {
        try {
            ObjectNode function = MAPPER.createObjectNode();
            function.put("name", getName());
            function.put("description", getDescription());
            function.set("parameters", MAPPER.readTree(getInputSchema()));

            ObjectNode schema = MAPPER.createObjectNode();
            schema.put("type", "function");
            schema.set("function", function);
            return MAPPER.writeValueAsString(schema);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getAnthropicSchema() {
        try {
            ObjectNode schema = MAPPER.createObjectNode();
            schema.put("name", getName());
            schema.put("description", getDescription());
            schema.set("input_schema", MAPPER.readTree(getInputSchema()));
            return MAPPER.writeValueAsString(schema);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
