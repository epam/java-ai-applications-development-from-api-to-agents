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
        //TODO: 1. Return the string "search_users".
        throw new TaskNotImplementedException();
    }

    @Override
    public String getDescription() {
        //TODO: 1. Return a clear description of the tool's purpose.
        //         Example: "Searches users by name, surname, email, and/or gender."
        throw new TaskNotImplementedException();
    }

    @Override
    public String getInputSchema() {
        //TODO: 1. Return a JSON Schema string defining the search parameters.
        //         All parameters should be optional: "name", "surname", "email" (strings), and "gender".
        //         Note: Use an enum for "gender" with values ["male", "female"].
        throw new TaskNotImplementedException();
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        //TODO:
        // 1. Extract the optional search parameters from the arguments Map:
        //    "name", "surname", "email", and "gender".
        // 2. Call userClient.searchUsers(name, surname, email, gender) and return the list of users found.
        // 3. Wrap in a try-catch to return an error message if the search fails.
        throw new TaskNotImplementedException();
    }
}
