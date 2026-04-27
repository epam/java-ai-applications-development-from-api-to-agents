package t10.mcp.advanced.mcp.server.server.tools.users;

import commons.user.service.UserServiceClient;
import commons.user.service.UserUpdate;
import commons.exceptions.TaskNotImplementedException;
import t10.mcp.advanced.mcp.server.server.tools.BaseUserServiceTool;

import java.util.Map;

public class UpdateUserTool extends BaseUserServiceTool {

    public UpdateUserTool(UserServiceClient userServiceClient) {
        super(userServiceClient);
    }

    @Override
    public String getName() {
        return "update_user";
    }

    @Override
    public String getDescription() {
        return "Updates user info";
    }

    @Override
    public String getInputSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "id": {"type": "number", "description": "User ID that should be updated."},
                    "new_info": {
                      "type": "object",
                      "properties": {
                        "name":          {"type": "string"},
                        "surname":       {"type": "string"},
                        "email":         {"type": "string"},
                        "phone":         {"type": "string"},
                        "date_of_birth": {"type": "string"},
                        "address": {
                          "type": "object",
                          "properties": {
                            "country":    {"type": "string"},
                            "city":       {"type": "string"},
                            "street":     {"type": "string"},
                            "flat_house": {"type": "string"}
                          }
                        },
                        "gender":  {"type": "string"},
                        "company": {"type": "string"},
                        "salary":  {"type": "number"},
                        "credit_card": {
                          "type": "object",
                          "properties": {
                            "num":      {"type": "string"},
                            "cvv":      {"type": "string"},
                            "exp_date": {"type": "string"}
                          }
                        }
                      }
                    }
                  },
                  "required": ["id"]
                }
                """;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String execute(Map<String, Object> arguments) {
        //TODO:
        // 1. Extract "id" and "new_info" map from arguments.
        // 2. Convert "new_info" to UserUpdate class using objectMapper.convertValue.
        // 3. Call userServiceClient.updateUser(userId, user) and return the result.
        throw new TaskNotImplementedException();
    }
}
