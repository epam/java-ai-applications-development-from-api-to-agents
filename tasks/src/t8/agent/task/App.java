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
        UserServiceClient userClient = new UserServiceClient();

        List<BaseTool> tools = List.of(
                new WebSearchTool(Constants.OPENAI_API_KEY),
                new GetUserByIdTool(userClient),
                new SearchUsersTool(userClient),
                new CreateUserTool(userClient),
                new UpdateUserTool(userClient),
                new DeleteUserTool(userClient)
        );

         BaseAgent agent = new OpenAIBasedAgent("gpt-5.2", Constants.OPENAI_API_KEY, tools, Prompts.SYSTEM_PROMPT);
//        BaseAgent agent = new AnthropicBasedAgent(Constants.CLAUDE_SONNET_4_5, Constants.ANTHROPIC_API_KEY, tools, Prompts.SYSTEM_PROMPT);

        Conversation conversation = new Conversation();

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

            conversation.addMessage(new Message(Role.USER, userInput));

            // mutable copy so the agent can append intermediate tool-call messages
            int prevSize = conversation.getMessages().size();
            List<Message> messages = new ArrayList<>(conversation.getMessages());

            Message aiMessage = agent.getResponse(messages, true);

            // sync intermediate messages (tool calls + tool results) back into conversation
            for (int i = prevSize; i < messages.size(); i++) {
                conversation.addMessage(messages.get(i));
            }
            conversation.addMessage(aiMessage);

            System.out.println("🤖: " + aiMessage.content());
            System.out.println("=".repeat(100));
            System.out.println();
            System.out.print("> ");
        }
    }
}
