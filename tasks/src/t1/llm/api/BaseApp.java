package t1.llm.api;


import commons.exceptions.TaskNotImplementedException;
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
        //TODO:
        // - Create a new Conversation instance to maintain chat history
        // - Create a Scanner reading from System.in
        // - Print a message telling the user how to exit (e.g., type "exit")
        // - Start a loop:
        //   - Print the input prompt ("=> ")
        //   - Read and strip the next line from Scanner
        //   - If the input equals "exit" (case-insensitive), print a goodbye message and break
        //   - Wrap the input in a new Message(Role.USER, ...) and add it to Conversation
        //   - Print "AI: " prefix (no newline)
        //   - Call client.streamResponse() or client.response() depending on the stream flag
        //   - Add the returned AI Message to Conversation

        Conversation conversation = new Conversation();

        Scanner scanner = new Scanner(System.in);
        System.out.println("Type \"exit\" to quit.");

        while (true) {
            System.out.print("=> ");
            String input = scanner.nextLine().strip();

            if (input.equalsIgnoreCase("exit")) {
                System.out.println("Goodbye!");
                break;
            }
            Message userMessage = new Message(Role.USER, input);
            conversation.addMessage(userMessage);

            System.out.print("AI: ");
            Message aiMessage = stream
                ? client.streamResponse(conversation.getMessages())
                : client.response(conversation.getMessages());

            conversation.addMessage(aiMessage);
        }
    }
}
