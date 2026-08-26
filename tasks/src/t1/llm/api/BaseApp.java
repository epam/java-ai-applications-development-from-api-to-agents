package t1.llm.api;

import java.util.Scanner;

import commons.model.Conversation;
import commons.model.Message;
import commons.model.Role;

/**
 * Interactive chat loop shared by all provider App classes.
 * <p>
 * Reads user input from stdin, maintains conversation history, calls the chosen client, and loops until the user types
 * "exit".
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

        var conversation = new Conversation();
        var scanner = new Scanner(System.in);
        System.out.println("To exit from the chat, type \"exit\"");

        while (true) {
            System.out.print("=> ");
            var prompt = scanner.nextLine();
            if (prompt.equals("exit")) {
                System.out.println("Good bye!");
                break;
            }

            var message = new Message(Role.USER, prompt);
            conversation.addMessage(message);
            System.out.print("AI: ");
            conversation.addMessage((stream) ? client.streamResponse(conversation.getMessages())
                : client.response(conversation.getMessages()));
        }
    }
}
