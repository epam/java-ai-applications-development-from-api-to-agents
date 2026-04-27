package t10.mcp.advanced.mcp.server.server.tools.users;

import commons.user.service.UserServiceClient;
import commons.exceptions.TaskNotImplementedException;
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
        //TODO:
        // 1. Extract the "id" from the arguments map.
        // 2. Convert it to int and call userServiceClient.getUser(userId).
        throw new TaskNotImplementedException();
    }
}
