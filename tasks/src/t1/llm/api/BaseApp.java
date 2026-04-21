package t1.llm.api;


import commons.model.Conversation;
import commons.model.Message;
import commons.model.Role;

import java.util.Scanner;

/**
 * Interactive chat loop shared by all provider App classes.
 * <p>
 * Reads user input from stdin, maintains conversation history, calls the chosen client,
 * and loops until the user types "exit".
 */
public class BaseApp {

    public static void start(boolean stream, AiClient client) {
        var conversation = new Conversation();
        var scanner = new Scanner(System.in);

        System.out.println("Type your question or 'exit' to quit.");
        while (true) {
            System.out.print("=> ");
            String userInput = scanner.nextLine().strip();

            if ("exit".equalsIgnoreCase(userInput)) {
                System.out.println("Exiting the chat. Goodbye!");
                break;
            }

            conversation.addMessage(new Message(Role.USER, userInput));

            System.out.print("AI: ");
            Message aiMessage = stream
                    ? client.streamResponse(conversation.getMessages())
                    : client.response(conversation.getMessages());

            conversation.addMessage(aiMessage);
        }
    }
}
