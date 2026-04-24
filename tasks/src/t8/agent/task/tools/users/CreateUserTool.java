package t8.agent.task.tools.users;

import com.fasterxml.jackson.databind.ObjectMapper;
import commons.user.service.UserCreate;
import commons.user.service.UserServiceClient;

import java.util.Map;

public class CreateUserTool extends BaseUserServiceTool {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public CreateUserTool(UserServiceClient userClient) {
        super(userClient);
    }

    @Override
    public String getName() {
        return "add_user";
    }

    @Override
    public String getDescription() {
        return "Adds new user";
    }

    @Override
    public String getInputSchema() {
        return """
                {
                    "type": "object",
                    "properties": {
                        "name":          { "type": "string" },
                        "surname":       { "type": "string" },
                        "email":         { "type": "string" },
                        "phone":         { "type": "string" },
                        "date_of_birth": { "type": "string" },
                        "address": {
                            "type": "object",
                            "properties": {
                                "country":    { "type": "string" },
                                "city":       { "type": "string" },
                                "street":     { "type": "string" },
                                "flat_house": { "type": "string" }
                            }
                        },
                        "gender":   { "type": "string" },
                        "company":  { "type": "string" },
                        "salary":   { "type": "number" },
                        "about_me": { "type": "string" },
                        "credit_card": {
                            "type": "object",
                            "properties": {
                                "num":      { "type": "string" },
                                "cvv":      { "type": "string" },
                                "exp_date": { "type": "string" }
                            }
                        }
                    },
                    "required": ["name", "surname", "email", "about_me"]
                }
                """;
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        try {
            UserCreate user = objectMapper.convertValue(arguments, UserCreate.class);
            return userClient.addUser(user);
        } catch (Exception e) {
            return "Error while creating a new user: " + e.getMessage();
        }
    }
}
