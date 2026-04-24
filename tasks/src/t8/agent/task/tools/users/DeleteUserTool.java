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
        //TODO: Return the tool name: "delete_users"
        throw new TaskNotImplementedException();
    }

    @Override
    public String getDescription() {
        //TODO: Return a short description of this tool's purpose.
        //      Example: "Deletes a user from the system by their ID."
        throw new TaskNotImplementedException();
    }

    @Override
    public String getInputSchema() {
        //TODO: Return a JSON Schema string for this tool's input.
        //      This tool accepts a single required parameter:
        //        - "id" (number): the ID of the user to delete
        //      Example:
        //        {
        //          "type": "object",
        //          "properties": {
        //            "id": { "type": "number", "description": "User ID" }
        //          },
        //          "required": ["id"]
        //        }
        throw new TaskNotImplementedException();
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        //TODO:
        // 1. Get the user ID: int id = ((Number) arguments.get("id")).intValue();
        // 2. Call userClient.deleteUser(id) and return its result
        // 3. Wrap in try-catch Exception:
        //    return "Error while deleting user by id: " + e.getMessage();
        throw new TaskNotImplementedException();
    }
}
