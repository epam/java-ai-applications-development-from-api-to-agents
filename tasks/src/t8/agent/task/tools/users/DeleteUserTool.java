package t8.agent.task.tools.users;

import commons.user.service.UserServiceClient;

import java.util.Map;

public class DeleteUserTool extends BaseUserServiceTool {

    public DeleteUserTool(UserServiceClient userClient) {
        super(userClient);
    }

    @Override
    public String getName() {
        return "delete_users";
    }

    @Override
    public String getDescription() {
        return "Deletes user from the system";
    }

    @Override
    public String getInputSchema() {
        return """
                {
                    "type": "object",
                    "properties": {
                        "id": {
                            "type": "number",
                            "description": "User ID"
                        }
                    },
                    "required": ["id"]
                }
                """;
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        try {
            int id = ((Number) arguments.get("id")).intValue();
            return userClient.deleteUser(id);
        } catch (Exception e) {
            return "Error while deleting user by id: " + e.getMessage();
        }
    }
}
