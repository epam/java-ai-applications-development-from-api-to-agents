package t10.mcp.advanced.mcp.server.server.tools.users;

import commons.user.service.UserServiceClient;
import t10.mcp.advanced.mcp.server.server.tools.BaseUserServiceTool;

import java.util.Map;

public class DeleteUserTool extends BaseUserServiceTool {

    public DeleteUserTool(UserServiceClient userServiceClient) {
        super(userServiceClient);
    }

    @Override
    public String getName() {
        return "delete_user";
    }

    @Override
    public String getDescription() {
        return "Deletes user by user_id";
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
        return userServiceClient.deleteUser(userId);
    }
}
