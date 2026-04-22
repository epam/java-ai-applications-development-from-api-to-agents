package t5.rag.advanced;

import commons.Constants;
import commons.model.Conversation;
import commons.model.Message;
import commons.model.Role;
import t5.rag.advanced.chat.ChatCompletionClient;
import t5.rag.advanced.embeddings.EmbeddingsClient;
import t5.rag.advanced.embeddings.SearchMode;
import t5.rag.advanced.embeddings.TextProcessor;

import java.util.List;
import java.util.Scanner;

public class App {

    private static final String MANUAL_PATH = "tasks/src/t5/rag/advanced/microwave_manual.txt";

    private static final String SYSTEM_PROMPT = """
            You are a RAG-powered assistant that assists users with their questions about microwave usage.

            ## Structure of User message:
            `RAG CONTEXT` - Retrieved documents relevant to the query.
            `USER QUESTION` - The user's actual question.

            ## Instructions:
            - Use information from `RAG CONTEXT` as context when answering the `USER QUESTION`.
            - Cite specific sources when using information from the context.
            - Answer ONLY based on conversation history and RAG context.
            - If no relevant information exists in `RAG CONTEXT` or conversation history, state that you cannot answer the question.
            """;

    private static final String USER_PROMPT_TEMPLATE =
            "##RAG CONTEXT:\n{context}\n\n\n##USER QUESTION: \n{query}";

    public static void main(String[] args) {
        System.out.println("🎯 Microwave RAG Assistant");
        System.out.println("=".repeat(100));

//        EmbeddingsClient embeddingsClient = new EmbeddingsClient(
//                Constants.OPENAI_EMBEDDINGS_ENDPOINT,
//                "text-embedding-3-small",
//                Constants.OPENAI_API_KEY
//        );
         EmbeddingsClient embeddingsClient = new EmbeddingsClient(
                 "http://localhost:11434/v1/embeddings",
                 "nomic-embed-text",
                 "ollama"
         );

        ChatCompletionClient completionClient = new ChatCompletionClient(
                Constants.OPENAI_CHAT_COMPLETIONS_ENDPOINT,
                Constants.GPT_5_4,
                Constants.OPENAI_API_KEY
        );

        TextProcessor textProcessor = new TextProcessor(
                embeddingsClient,
                "localhost", 5433, "vectordb", "postgres", "postgres"
        );

        Scanner scanner = new Scanner(System.in);
        System.out.print("\nLoad context to VectorDB (y/n)? > ");
        System.out.flush();
        String loadContext = scanner.nextLine().strip();
        if (loadContext.equalsIgnoreCase("y") || loadContext.equalsIgnoreCase("yes")) {
            textProcessor.processTextFile(MANUAL_PATH, 400, 40, 384);
            System.out.println("=".repeat(100));
        }

        Conversation conversation = new Conversation();
        conversation.addMessage(new Message(Role.SYSTEM, SYSTEM_PROMPT));

        while (true) {
            System.out.print("\n➡️ ");
            System.out.flush();
            if (!scanner.hasNextLine()) break;
            String userRequest = scanner.nextLine().strip();
            if (userRequest.isEmpty()) continue;
            if (userRequest.equalsIgnoreCase("quit") || userRequest.equalsIgnoreCase("exit")) {
                System.out.println("👋 Goodbye");
                break;
            }

            // Step 1: Retrieval
            System.out.printf("%s%n🔍 STEP 1: RETRIEVAL%n%s%n", "=".repeat(100), "-".repeat(100));
            List<String> context = textProcessor.search(
                    SearchMode.EUCLIDEAN_DISTANCE,
                    userRequest,
                    5,
                    0.01,
                    384
            );

            // Step 2: Augmentation
            System.out.printf("%n%s%n🔗 STEP 2: AUGMENTATION%n%s%n", "=".repeat(100), "-".repeat(100));
            String augmentedPrompt = USER_PROMPT_TEMPLATE
                    .replace("{context}", String.join("\n\n", context))
                    .replace("{query}", userRequest);
            conversation.addMessage(new Message(Role.USER, augmentedPrompt));
            System.out.println("Prompt:\n" + augmentedPrompt);

            // Step 3: Generation
            System.out.printf("%n%s%n🤖 STEP 3: GENERATION%n%s%n", "=".repeat(100), "-".repeat(100));
            Message aiMessage = completionClient.getCompletion(conversation.getMessages());
            System.out.println("✅ RESPONSE:\n" + aiMessage.content());
            System.out.println("=".repeat(100));
            conversation.addMessage(aiMessage);
        }
    }
}
