package t6.grounding.t2;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import commons.Constants;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import t6.grounding.User;
import t6.grounding.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class InputVectorBasedGroundingApp {

    private static final String SYSTEM_PROMPT = """
            You are a RAG-powered assistant that assists users with their questions about user information.

            ## Structure of User message:
            `RAG CONTEXT` - Retrieved documents relevant to the query.
            `USER QUESTION` - The user's actual question.

            ## Instructions:
            - Use information from `RAG CONTEXT` as context when answering the `USER QUESTION`.
            - Cite specific sources when using information from the context.
            - Answer ONLY based on conversation history and RAG context.
            - If no relevant information exists in `RAG CONTEXT` or conversation history, state that you cannot answer the question.
            """;

    private static final String USER_PROMPT = """
            ##RAG CONTEXT:
            {context}

            ##USER QUESTION:\s
            {query}""";

    private final OpenAIClient openAiClient;
    private final SimpleVectorStore vectorStore;

    public InputVectorBasedGroundingApp(OpenAiEmbeddingModel embeddingModel) {
        this.openAiClient = OpenAIOkHttpClient.builder()
                .apiKey(Constants.OPENAI_API_KEY)
                .build();
        this.vectorStore = buildVectorStore(embeddingModel);
    }

    private SimpleVectorStore buildVectorStore(OpenAiEmbeddingModel embeddingModel) {
        System.out.println("🔎 Loading all users...");
        UserService userService = new UserService();
        List<User> users = userService.getAllUsers();

        System.out.printf("↗️ Formatting %d user documents and creating embeddings...%n", users.size());
        List<Document> documents = users.stream()
                .map(u -> Document.builder()
                        .id(String.valueOf(u.id()))
                        .text(u.toDocument())
                        .build())
                .collect(Collectors.toList());

        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
        addInParallel(store, documents, 50);
        System.out.println("✅ Vectorstore is ready.");
        return store;
    }

    private static void addInParallel(SimpleVectorStore store, List<Document> documents, int batchSize) {
        List<List<Document>> batches = new ArrayList<>();
        for (int i = 0; i < documents.size(); i += batchSize) {
            batches.add(documents.subList(i, Math.min(i + batchSize, documents.size())));
        }
        List<CompletableFuture<Void>> futures = batches.stream()
                .map(batch -> CompletableFuture.runAsync(() -> store.add(batch)))
                .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private String retrieveContext(String query, int k, double minScore) {
        System.out.println("Retrieving context...");

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(k)
                .similarityThreshold(minScore)
                .build();

        List<Document> results = vectorStore.similaritySearch(request);

        List<String> contextParts = results.stream()
                .map(doc -> {
                    System.out.printf("Retrieved (Score: %.3f): %s%n", doc.getScore(), doc.getText());
                    return doc.getText();
                })
                .collect(Collectors.toList());

        System.out.println("=".repeat(100));
        return String.join("\n\n", contextParts);
    }

    private String augmentPrompt(String query, String context) {
        return USER_PROMPT
                .replace("{context}", context)
                .replace("{query}", query);
    }

    private String generateAnswer(String augmentedPrompt) {
        var params = ChatCompletionCreateParams.builder()
                .model(Constants.GPT_4O_MINI)
                .temperature(0.0)
                .addSystemMessage(SYSTEM_PROMPT)
                .addUserMessage(augmentedPrompt)
                .build();

        var completion = openAiClient.chat().completions().create(params);
        return completion.choices().get(0).message().content().orElse("");
    }

    public static void main(String[] args) {
        OpenAiEmbeddingModel embeddingModel = new OpenAiEmbeddingModel(
                OpenAiApi.builder()
                        .apiKey(Constants.OPENAI_API_KEY)
                        .build(),
                MetadataMode.EMBED,
                OpenAiEmbeddingOptions.builder()
                        .model("text-embedding-3-small")
                        .dimensions(384)
                        .build()
        );

        InputVectorBasedGroundingApp app = new InputVectorBasedGroundingApp(embeddingModel);

        System.out.println("Query samples:");
        System.out.println(" - I need user emails that filled with hiking and psychology");
        System.out.println(" - Who is John?");

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("\n> ");
            System.out.flush();
            if (!scanner.hasNextLine()) break;
            String query = scanner.nextLine().strip();
            if (query.isEmpty()) continue;
            if (query.equalsIgnoreCase("quit") || query.equalsIgnoreCase("exit")) break;

            String context = app.retrieveContext(query, 10, 0.1);
            String augmented = app.augmentPrompt(query, context);
            String answer = app.generateAnswer(augmented);
            System.out.println(answer);
        }
    }
}

// The problems with Vector based Grounding approach are:
//   - In current solution we fetched all users once, prepared Vector store (Embed takes money) but we didn't play
//     around the point that new users added and deleted every 5 minutes. (Actually, it can be fixed, we can create once
//     Vector store and with new request we will fetch all the users, compare new and deleted with version in Vector
//     store and delete the data about deleted users and add new users).
//   - Limit with top_k (we can set up to 100, but what if the real number of similarity search 100+?)
//   - With some requests works not so perfectly.
//   - Need to play with balance between top_k and score_threshold
// Benefits are:
//   - Similarity search by context
//   - Any input can be used for search
//   - Costs reduce
