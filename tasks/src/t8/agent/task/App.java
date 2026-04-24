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
        // 1. Create UserServiceClient:
        //    UserServiceClient userClient = new UserServiceClient();
        // 2. Create the tool list — each tool gets the resources it needs:
        //    List<BaseTool> tools = List.of(
        //        new WebSearchTool(Constants.OPENAI_API_KEY),
        //        new GetUserByIdTool(userClient),
        //        new SearchUsersTool(userClient),
        //        new CreateUserTool(userClient),
        //        new UpdateUserTool(userClient),
        //        new DeleteUserTool(userClient)
        //    );
        // 3. Create the agent — pick one (comment the other out to switch providers):
        //    BaseAgent agent = new OpenAIBasedAgent(Constants.GPT_5_4, Constants.OPENAI_API_KEY, tools, Prompts.SYSTEM_PROMPT);
        //    // BaseAgent agent = new AnthropicBasedAgent(Constants.CLAUDE_SONNET_4_5, Constants.ANTHROPIC_API_KEY, tools, Prompts.SYSTEM_PROMPT);
        // 4. Create a Conversation to maintain history across turns:
        //    Conversation conversation = new Conversation();

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
            // 1. Add the user's message to the conversation:
            //    conversation.addMessage(new Message(Role.USER, userInput));
            // 2. Create a mutable copy so the agent can append intermediate tool-call messages.
            //    Record the current size first to know which messages were added by the agent:
            //    int prevSize = conversation.getMessages().size();
            //    List<Message> messages = new ArrayList<>(conversation.getMessages());
            // 3. Call the agent with the current conversation history:
            //    Message aiMessage = agent.getResponse(messages, true);
            // 4. Sync intermediate messages (tool calls + tool results) back into conversation:
            //    for (int i = prevSize; i < messages.size(); i++) { conversation.addMessage(messages.get(i)); }
            // 5. Add the final AI response to conversation and print it:
            //    conversation.addMessage(aiMessage);
            //    System.out.println("🤖: " + aiMessage.content());

            System.out.println("=".repeat(100));
            System.out.println();
            System.out.print("> ");
        }
    }
}
