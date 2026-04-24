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
        //TODO: Return the tool name: "get_user_by_id"
        throw new TaskNotImplementedException();
    }

    @Override
    public String getDescription() {
        //TODO: Return a short description of this tool's purpose.
        //      Example: "Retrieves full information about a user by their ID."
        throw new TaskNotImplementedException();
    }

    @Override
    public String getInputSchema() {
        //TODO: Return a JSON Schema string for this tool's input.
        //      This tool accepts a single required parameter:
        //        - "id" (number): the ID of the user to retrieve
        //      Use the same schema structure as DeleteUserTool.
        throw new TaskNotImplementedException();
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        //TODO:
        // 1. Get the user ID: int id = ((Number) arguments.get("id")).intValue();
        // 2. Call userClient.getUser(id) and return its result
        // 3. Wrap in try-catch Exception:
        //    return "Error while retrieving user by id: " + e.getMessage();
        throw new TaskNotImplementedException();
    }
}
