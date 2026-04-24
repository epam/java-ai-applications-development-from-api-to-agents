package t8.agent.task.tools.users;

import com.fasterxml.jackson.databind.ObjectMapper;
import commons.exceptions.TaskNotImplementedException;
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
        //TODO: Return the tool name: "add_user"
        throw new TaskNotImplementedException();
    }

    @Override
    public String getDescription() {
        //TODO: Return a short description of this tool's purpose.
        //      Example: "Adds a new user to the system."
        throw new TaskNotImplementedException();
    }

    @Override
    public String getInputSchema() {
        //TODO: Return a JSON Schema string describing the fields for user creation.
        //      Include all fields a user record can have:
        //        - name, surname, email, phone, date_of_birth (strings)
        //        - gender, company, about_me (strings)
        //        - salary (number)
        //        - address (object: country, city, street, flat_house — all strings)
        //        - credit_card (object: num, cvv, exp_date — all strings)
        //      Required fields: "name", "surname", "email", "about_me"
        //      Use a text block: return """ { ... } """;
        throw new TaskNotImplementedException();
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        //TODO:
        // 1. Convert arguments map to UserCreate using Jackson:
        //    UserCreate user = objectMapper.convertValue(arguments, UserCreate.class);
        // 2. Call userClient.addUser(user) and return its result (a String response from the service)
        // 3. Wrap in try-catch Exception:
        //    return "Error while creating a new user: " + e.getMessage();
        throw new TaskNotImplementedException();
    }
}
