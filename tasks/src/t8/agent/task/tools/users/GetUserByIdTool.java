package t8.agent.task.tools.users;

import commons.exceptions.TaskNotImplementedException;
import commons.user.service.UserServiceClient;

import java.util.Map;

public class GetUserByIdTool extends BaseUserServiceTool {

    public GetUserByIdTool(UserServiceClient userClient) {
        super(userClient);
    }

    @Override
    public String getName() {
        //TODO: 1. Return the string "get_user_by_id".
        throw new TaskNotImplementedException();
    }

    @Override
    public String getDescription() {
        //TODO: 1. Return a clear description of the tool's purpose.
        //         Example: "Retrieves full information about a user by their ID."
        throw new TaskNotImplementedException();
    }

    @Override
    public String getInputSchema() {
        //TODO: 1. Return a JSON Schema string for the input.
        //         The tool expects an object with one required number property: "id".
        throw new TaskNotImplementedException();
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        //TODO:
        // 1. Extract the user ID from the arguments Map.
        //    Note: Cast arguments.get("id") to Number before calling intValue().
        // 2. Call userClient.getUser(id) and return the resulting user data.
        // 3. Wrap in a try-catch to return an error message if the retrieval fails.
        throw new TaskNotImplementedException();
    }
}
