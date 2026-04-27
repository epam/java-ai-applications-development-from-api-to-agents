package t10.mcp.advanced.mcp.server.server.tools;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class BaseTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public abstract String getName();

    public abstract String getDescription();

    public abstract String getInputSchema();

    public abstract String execute(Map<String, Object> arguments);

    public Map<String, Object> toMcpTool() {
        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("name", getName());
        tool.put("description", getDescription());
        try {
            tool.put("inputSchema", MAPPER.readValue(getInputSchema(), Map.class));
        } catch (Exception e) {
            throw new RuntimeException("Invalid inputSchema JSON in tool: " + getName(), e);
        }
        return tool;
    }
}
