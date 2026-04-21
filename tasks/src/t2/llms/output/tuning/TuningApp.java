package t2.llms.output.tuning;

import commons.model.Conversation;
import commons.model.Message;
import commons.model.Role;
import t2.llms.output.tuning.clients.AIClient;

import java.util.Map;
import java.util.Scanner;

public final class TuningApp {

    private TuningApp() {
    }

    public static void run(
            AIClient client,
            boolean printRequest,
            boolean printOnlyContent,
            Map<String, Object> options
    ) {
        var conversation = new Conversation();
        var scanner = new Scanner(System.in);

        System.out.println("Type your question or 'exit' to quit.");
        while (true) {
            System.out.print("> ");
            if (!scanner.hasNextLine()) {
                System.out.println("Exiting the chat. Goodbye!");
                return;
            }

            var userInput = scanner.nextLine().trim();
            if ("exit".equalsIgnoreCase(userInput)) {
                System.out.println("Exiting the chat. Goodbye!");
                return;
            }

            conversation.addMessage(new Message(Role.USER, userInput));
            System.out.println("AI:");
            var aiMessage = client.response(conversation.getMessages(), printRequest, printOnlyContent, options);
            conversation.addMessage(aiMessage);
        }
    }
}
