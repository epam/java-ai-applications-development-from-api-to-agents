package t10.mcp.advanced.mcp.server.server.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import commons.exceptions.TaskNotImplementedException;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class BaseTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public abstract String getName();

    public abstract String getDescription();

    public abstract String getInputSchema();

    public abstract String execute(Map<String, Object> arguments);

    public Map<String, Object> toMcpTool() {
        //TODO:
        // 1. Create a LinkedHashMap to represent the MCP tool structure.
        // 2. Put "name" and "description" from the tool's methods.
        // 3. Parse the "inputSchema" JSON string into a Map.
        //    Snippet: MAPPER.readValue(getInputSchema(), Map.class)
        // 4. Put the parsed map under the key "inputSchema".
        throw new TaskNotImplementedException();
    }
}
