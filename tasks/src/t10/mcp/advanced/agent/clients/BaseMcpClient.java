package t10.mcp.advanced.agent.clients;

import java.util.List;
import java.util.Map;

public abstract class BaseMcpClient {

    protected final String serverUrl;

    protected BaseMcpClient(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public abstract List<Map<String, Object>> getTools();

    public abstract String callTool(String name, Map<String, Object> arguments);
}
