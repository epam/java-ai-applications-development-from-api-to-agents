package t8.agent.task.tools.users;

import commons.exceptions.TaskNotImplementedException;
import commons.user.service.UserServiceClient;

import java.util.Map;

public class SearchUsersTool extends BaseUserServiceTool {

    public SearchUsersTool(UserServiceClient userClient) {
        super(userClient);
    }

    @Override
    public String getName() {
        //TODO: Return the tool name: "search_users"
        throw new TaskNotImplementedException();
    }

    @Override
    public String getDescription() {
        //TODO: Return a short description of this tool's purpose.
        //      Example: "Searches users by name, surname, email, and/or gender."
        throw new TaskNotImplementedException();
    }

    @Override
    public String getInputSchema() {
        //TODO: Return a JSON Schema string for this tool's input.
        //      All parameters are optional — none are required ("required": []):
        //        - "name" (string): user first name
        //        - "surname" (string): user last name
        //        - "email" (string): user email address
        //        - "gender" (string): restricted values — use "enum": ["male", "female"]
        throw new TaskNotImplementedException();
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        //TODO:
        // 1. Extract optional parameters (each may be null if not provided by the model):
        //    String name    = (String) arguments.get("name");
        //    String surname = (String) arguments.get("surname");
        //    String email   = (String) arguments.get("email");
        //    String gender  = (String) arguments.get("gender");
        // 2. Call userClient.searchUsers(name, surname, email, gender) and return its result
        // 3. Wrap in try-catch Exception:
        //    return "Error while searching users: " + e.getMessage();
        throw new TaskNotImplementedException();
    }
}
