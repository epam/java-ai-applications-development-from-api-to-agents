package t10.mcp.advanced.mcp.server.server.tools.users;

import commons.user.service.UserCreate;
import commons.user.service.UserServiceClient;
import commons.exceptions.TaskNotImplementedException;
import t10.mcp.advanced.mcp.server.server.tools.BaseUserServiceTool;

import java.util.Map;

public class CreateUserTool extends BaseUserServiceTool {

    public CreateUserTool(UserServiceClient userServiceClient) {
        super(userServiceClient);
    }

    @Override
    public String getName() {
        return "add_user";
    }

    @Override
    public String getDescription() {
        return "Adds new user into the system";
    }

    @Override
    public String getInputSchema() {
        return """
                {
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
                    "gender":  {"type": "string", "enum": ["male", "female"]},
                    "company": {"type": "string"},
                    "salary":  {"type": "number"},
                    "about_me": {"type": "string"},
                    "credit_card": {
                      "type": "object",
                      "properties": {
                        "num":      {"type": "string"},
                        "cvv":      {"type": "string"},
                        "exp_date": {"type": "string"}
                      }
                    }
                  },
                  "required": ["name", "surname", "email", "about_me"]
                }
                """;
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        //TODO:
        // 1. Convert the arguments map to UserCreate class using objectMapper.convertValue.
        // 2. Call userServiceClient.addUser(user) and return the result string.
        throw new TaskNotImplementedException();
    }
}
