package t10.mcp.advanced.mcp.server.server.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import commons.user.service.UserServiceClient;

public abstract class BaseUserServiceTool extends BaseTool {

    protected final UserServiceClient userServiceClient;
    protected final ObjectMapper objectMapper;

    protected BaseUserServiceTool(UserServiceClient userServiceClient) {
        this.userServiceClient = userServiceClient;
        this.objectMapper = new ObjectMapper();
    }
}
