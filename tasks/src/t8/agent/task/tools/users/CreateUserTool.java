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
        //TODO: 1. Return the string "add_user".
        throw new TaskNotImplementedException();
    }

    @Override
    public String getDescription() {
        //TODO: 1. Return a clear description of the tool's purpose.
        //         Example: "Adds a new user to the system."
        throw new TaskNotImplementedException();
    }

    @Override
    public String getInputSchema() {
        //TODO: 1. Return a JSON Schema string defining the user creation fields.
        //         Include: name, surname, email, phone, date_of_birth, gender, company, about_me (strings), salary (number).
        //         Also include nested objects: address (country, city, street, flat_house) and credit_card (num, cvv, exp_date).
        //         Note: Set "name", "surname", "email", and "about_me" as required fields.
        throw new TaskNotImplementedException();
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        //TODO:
        // 1. Convert the arguments Map into a UserCreate object.
        //    Use objectMapper.convertValue(arguments, UserCreate.class).
        // 2. Call userClient.addUser(user) and return the response string.
        // 3. Wrap in a try-catch to return an error message if the creation fails.
        throw new TaskNotImplementedException();
    }
}
