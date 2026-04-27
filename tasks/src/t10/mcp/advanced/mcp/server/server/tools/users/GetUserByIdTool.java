package t10.mcp.advanced.mcp.server.server.tools.users;

import commons.user.service.UserServiceClient;
import t10.mcp.advanced.mcp.server.server.tools.BaseUserServiceTool;

import java.util.Map;

public class GetUserByIdTool extends BaseUserServiceTool {

    public GetUserByIdTool(UserServiceClient userServiceClient) {
        super(userServiceClient);
    }

    @Override
    public String getName() {
        return "get_user_by_id";
    }

    @Override
    public String getDescription() {
        return "Provides full user information by user_id";
    }

    @Override
    public String getInputSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "id": {"type": "number", "description": "User ID"}
                  },
                  "required": ["id"]
                }
                """;
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        int userId = ((Number) arguments.get("id")).intValue();
        return userServiceClient.getUser(userId);
    }
}
