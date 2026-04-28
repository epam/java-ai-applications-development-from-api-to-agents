package t12.skills.task.custom.tools;

import t12.skills.task.custom.FileUtils;
import t12.skills.task.custom.mcp.McpClient;
import t12.skills.task.custom.mcp.McpToolModel;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PythonCodeInterpreter extends BaseTool {

    private static final String SCRIPT_PATH_PARAM = "script_path";

    private final McpClient mcpClient;
    private final Path skillsDir;
    private final String toolName;
    private final String toolDescription;
    private final Map<String, Object> toolParameters;

    private PythonCodeInterpreter(McpClient mcpClient, Path skillsDir,
                                   String toolName, String toolDescription,
                                   Map<String, Object> toolParameters) {
        this.mcpClient = mcpClient;
        this.skillsDir = skillsDir.toAbsolutePath().normalize();
        this.toolName = toolName;
        this.toolDescription = toolDescription;
        this.toolParameters = toolParameters;
    }

    /**
     * Factory method mirroring PythonCodeInterpreterTool.create() in the Python version.
     * Fetches tool models from the MCP server, finds the target tool by name, validates it
     * exists, then constructs the instance with the server-provided schema augmented by
     * a script_path property.
     */
    @SuppressWarnings("unchecked")
    public static PythonCodeInterpreter create(McpClient mcpClient, Path skillsDir, String toolName) {
        List<McpToolModel> mcpTools = mcpClient.listTools();

        McpToolModel tool = mcpTools.stream()
                .filter(t -> toolName.equals(t.name()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "MCP server doesn't have `" + toolName + "` tool. Available: "
                        + mcpTools.stream().map(McpToolModel::name).toList()));

        Map<String, Object> properties = new HashMap<>((Map<String, Object>) tool.parameters().get("properties"));
        properties.put(SCRIPT_PATH_PARAM, Map.of(
                "type", "string",
                "description", "Path with python script to upload to code interpreter. "
                        + "Will be combined with `code` in such way: "
                        + "code from file by `script_path` + \\n\\n + `code`."
        ));
        Map<String, Object> augmentedParameters = new HashMap<>(tool.parameters());
        augmentedParameters.put("properties", properties);

        return new PythonCodeInterpreter(mcpClient, skillsDir, tool.name(), tool.description(), augmentedParameters);
    }

    @Override
    public String getName() {
        return toolName;
    }

    @Override
    public String getDescription() {
        return toolDescription;
    }

    @Override
    public Map<String, Object> getParameters() {
        return toolParameters;
    }

    @Override
    protected String doExecute(Map<String, Object> arguments) {
        Map<String, Object> args;

        if (arguments.get(SCRIPT_PATH_PARAM) != null) {
            String rawPath = ((String) arguments.get(SCRIPT_PATH_PARAM)).replaceFirst("^/+", "");
            Path fullPath = skillsDir.resolve(rawPath).normalize();
            String scriptContent = FileUtils.getFileContent(fullPath);
            args = new HashMap<>();
            args.put("code", scriptContent + "\n\n" + arguments.getOrDefault("code", ""));
            args.put("session_id", arguments.getOrDefault("session_id", ""));
        } else {
            args = arguments;
        }

        return mcpClient.callTool(toolName, args);
    }
}
