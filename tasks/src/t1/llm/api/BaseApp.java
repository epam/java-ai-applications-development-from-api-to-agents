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
        // TODO:
        // 1. Create a Conversation instance and a Scanner reading from System.in
        // 2. Print a message telling the user how to exit
        // 3. Loop forever:
        //    3.1. Print "=> " prompt (no newline) and read the next line from scanner (strip whitespace)
        //    3.2. If "exit".equalsIgnoreCase(userInput) → print goodbye message and break
        //    3.3. Add a new Message(Role.USER, userInput) to the conversation
        //    3.4. Print "AI: " (no newline), then call either client.streamResponse() or client.response()
        //         passing conversation.getMessages(); which to call depends on the stream flag
        //    3.5. Add the returned AI Message to the conversation
        throw new TaskNotImplementedException();
    }
}