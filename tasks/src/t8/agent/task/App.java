package t8.agent.task;

import commons.Constants;
import commons.model.Conversation;
import commons.model.Message;
import commons.model.Role;
import commons.user.service.UserServiceClient;
import t8.agent.task.agents.AnthropicBasedAgent;
import t8.agent.task.agents.BaseAgent;
import t8.agent.task.agents.OpenAIBasedAgent;
import t8.agent.task.tools.BaseTool;
import t8.agent.task.tools.WebSearchTool;
import t8.agent.task.tools.users.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class App {

    public static void main(String[] args) {
        //TODO:
        // 1. Create a new UserServiceClient to interact with the backend user service
        // 2. Initialize the list of tools available to the agent.
        //    Each tool requires specific resources (API keys or service clients):
        //        - WebSearchTool(Constants.OPENAI_API_KEY),
        //        - GetUserByIdTool(userClient),
        //        - SearchUsersTool(userClient),
        //        - CreateUserTool(userClient),
        //        - UpdateUserTool(userClient),
        //        - DeleteUserTool(userClient)
        // 3. Instantiate the BaseAgent. Pick either OpenAIBasedAgent or AnthropicBasedAgent
        // 4. Create a new Conversation object to track the message history across turns

        System.out.println("Type your question or 'exit' to quit.");
        System.out.println("Sample:");
        System.out.println("`What tools do you have?`");
        System.out.println("`Add Andrej Karpathy as a new user`");
        System.out.print("> ");

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String userInput = scanner.nextLine().strip();

            if ("exit".equalsIgnoreCase(userInput)) {
                System.out.println("Exiting the chat. Goodbye!");
                break;
            }

            //TODO:
            // 1. Wrap the user input in a Message with Role.USER and add it to the conversation.
            // 2. Prepare a mutable copy of the current message history.
            //    Note: The agent will append intermediate messages (tool calls and results) to this list.
            // 3. Capture the current size of the message list before calling the agent.
            //    This helps in identifying new intermediate messages added during the turn:
            //    int prevSize = messages.size();
            // 4. Request a response from the agent:
            //    Message aiMessage = agent.getResponse(messages, true);
            // 5. Sync any new intermediate messages (indices from prevSize onwards) back into the conversation object.
            // 6. Finally, add the definitive AI response (aiMessage) to the conversation and print it to the console.

            System.out.println("=".repeat(100));
            System.out.println();
            System.out.print("> ");
        }
    }
}
