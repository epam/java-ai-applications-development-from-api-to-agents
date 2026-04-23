package t6.grounding.t1;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import commons.Constants;
import t6.grounding.User;
import t6.grounding.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class NoGroundingApp {

    private static final String BATCH_SYSTEM_PROMPT = """
            You are a user search assistant. Your task is to find users from the provided list that match the search criteria.

            INSTRUCTIONS:
            1. Analyze the user question to understand what attributes/characteristics are being searched for
            2. Examine each user in the context and determine if they match the search criteria
            3. For matching users, extract and return their complete information
            4. Be inclusive - if a user partially matches or could potentially match, include them

            OUTPUT FORMAT:
            - If you find matching users: Return their full details exactly as provided, maintaining the original format
            - If no users match: Respond with exactly "NO_MATCHES_FOUND"
            - If uncertain about a match: Include the user with a note about why they might match""";

    private static final String FINAL_SYSTEM_PROMPT = """
            You are a helpful assistant that provides comprehensive answers based on user search results.

            INSTRUCTIONS:
            1. Review all the search results from different user batches
            2. Combine and deduplicate any matching users found across batches
            3. Present the information in a clear, organized manner
            4. If multiple users match, group them logically
            5. If no users match, explain what was searched for and suggest alternatives""";

    private static final String USER_PROMPT = """
            ## USER DATA:
            {context}

            ## SEARCH QUERY:\s
            {query}""";

    private final OpenAIClient openAiClient;
    private final UserService userService;
    private final AtomicInteger totalTokens = new AtomicInteger(0);
    private final List<Integer> batchTokens = new CopyOnWriteArrayList<>();

    public NoGroundingApp() {
        this.openAiClient = OpenAIOkHttpClient.builder()
                .apiKey(Constants.OPENAI_API_KEY)
                .build();
        this.userService = new UserService();
    }

    private String generateResponse(String systemPrompt, String userMessage) {
        System.out.println("Processing...");

        var params = ChatCompletionCreateParams.builder()
                .model(Constants.GPT_4_1_NANO)
                .temperature(0.0)
                .addSystemMessage(systemPrompt)
                .addUserMessage(userMessage)
                .build();

        var completion = openAiClient.chat().completions().create(params);
        int usedTokens = completion.usage()
                .map(u -> (int) u.totalTokens())
                .orElse(0);

        totalTokens.addAndGet(usedTokens);
        batchTokens.add(usedTokens);

        String content = completion.choices().get(0).message().content().orElse("");
        System.out.println("Response: \n " + content + "\nTokens used: " + usedTokens + "\n");
        return content;
    }

    public void run(String userQuestion) {
        System.out.println("\n--- Searching user database ---");

        List<User> users = userService.getAllUsers();

        List<List<User>> batches = new ArrayList<>();
        for (int i = 0; i < users.size(); i += 100) {
            batches.add(users.subList(i, Math.min(i + 100, users.size())));
        }

        List<CompletableFuture<String>> futures = batches.stream()
                .map(batch -> CompletableFuture.supplyAsync(() -> generateResponse(
                        BATCH_SYSTEM_PROMPT,
                        USER_PROMPT
                                .replace("{context}", batch.stream()
                                        .map(User::toDocument)
                                        .collect(Collectors.joining("\n")))
                                .replace("{query}", userQuestion)
                )))
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        List<String> batchResults = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        System.out.println("\n--- Compiling results ---");

        List<String> relevantResults = batchResults.stream()
                .filter(r -> !r.strip().equals("NO_MATCHES_FOUND"))
                .collect(Collectors.toList());

        System.out.println("\n=== SEARCH RESULTS ===");
        if (!relevantResults.isEmpty()) {
            String combined = String.join("\n\n", relevantResults);
            generateResponse(
                    FINAL_SYSTEM_PROMPT,
                    "SEARCH RESULTS:\n" + combined + "\n\nORIGINAL QUERY: " + userQuestion
            );
        } else {
            System.out.println("No users found matching '" + userQuestion + "'");
            System.out.println("\nTry refining your search or using different keywords.");
        }

        System.out.println("\n=== Performance ===");
        System.out.println("Total API calls: " + batchTokens.size());
        System.out.println("Total tokens: " + totalTokens.get());
    }

    public static void main(String[] args) {
        NoGroundingApp app = new NoGroundingApp();

        System.out.println("Query samples:");
        System.out.println(" - Do we have someone with name John that loves traveling?");

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("\n> ");
            System.out.flush();
            if (!scanner.hasNextLine()) break;
            String query = scanner.nextLine().strip();
            if (query.isEmpty()) continue;
            app.run(query);
        }
    }
}

// The problems with No Grounding approach are:
//   - If we load whole users as context in one request to LLM we will hit context window
//   - Huge token usage == Higher price per request
//   - Added + one chain in flow where original user data can be changed by LLM (before final generation)
// User Question -> Get all users -> ‼️parallel search of possible candidates‼️ -> probably changed original context -> final generation
