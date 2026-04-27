package t8.agent.task.tools.users;

import commons.exceptions.TaskNotImplementedException;
import commons.user.service.UserServiceClient;

import java.util.Map;

public class DeleteUserTool extends BaseUserServiceTool {

    public DeleteUserTool(UserServiceClient userClient) {
        super(userClient);
    }

    @Override
    public String getName() {
        //TODO: 1. Return the string "delete_users".
        throw new TaskNotImplementedException();
    }

    @Override
    public String getDescription() {
        //TODO: 1. Return a clear description of the tool's purpose.
        //         Example: "Deletes a user from the system by their ID."
        throw new TaskNotImplementedException();
    }

    @Override
    public String getInputSchema() {
        //TODO: 1. Return a JSON Schema string defining the input.
        //         The tool expects an object with one required number property: "id".
        throw new TaskNotImplementedException();
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        //TODO:
        // 1. Extract the user ID from the arguments Map.
        //    Note: Cast arguments.get("id") to Number before calling intValue().
        // 2. Call userClient.deleteUser(id) and return the response.
        // 3. Wrap in a try-catch to handle potential deletion errors.
        throw new TaskNotImplementedException();
    }
}
