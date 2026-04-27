package t10.mcp.advanced.mcp.server.server.tools.users;

import commons.user.service.UserServiceClient;
import commons.exceptions.TaskNotImplementedException;
import t10.mcp.advanced.mcp.server.server.tools.BaseUserServiceTool;

import java.util.Map;

public class SearchUsersTool extends BaseUserServiceTool {

    public SearchUsersTool(UserServiceClient userServiceClient) {
        super(userServiceClient);
    }

    @Override
    public String getName() {
        return "search_users";
    }

    @Override
    public String getDescription() {
        return "Searches users by name, surname, email, and gender";
    }

    @Override
    public String getInputSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "name":    {"type": "string", "description": "User name"},
                    "surname": {"type": "string", "description": "User surname"},
                    "email":   {"type": "string", "description": "User email"},
                    "gender":  {"type": "string", "description": "User gender", "enum": ["male", "female"]}
                  },
                  "required": []
                }
                """;
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        //TODO:
        // 1. Extract search parameters (name, surname, email, gender) from the arguments map.
        // 2. Call userServiceClient.searchUsers with these parameters and return the result.
        throw new TaskNotImplementedException();
    }
}
