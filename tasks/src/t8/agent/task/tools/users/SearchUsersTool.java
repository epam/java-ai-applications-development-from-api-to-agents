package t8.agent.task.tools.users;

import commons.user.service.UserServiceClient;

import java.util.Map;

public class SearchUsersTool extends BaseUserServiceTool {

    public SearchUsersTool(UserServiceClient userClient) {
        super(userClient);
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
                        "name":    { "type": "string", "description": "User name" },
                        "surname": { "type": "string", "description": "User surname" },
                        "email":   { "type": "string", "description": "User email" },
                        "gender":  { "type": "string", "description": "User gender", "enum": ["male", "female"] }
                    },
                    "required": []
                }
                """;
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        try {
            String name = (String) arguments.get("name");
            String surname = (String) arguments.get("surname");
            String email = (String) arguments.get("email");
            String gender = (String) arguments.get("gender");
            return userClient.searchUsers(name, surname, email, gender);
        } catch (Exception e) {
            return "Error while searching users: " + e.getMessage();
        }
    }
}
