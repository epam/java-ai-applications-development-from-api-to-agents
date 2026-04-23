package t6.grounding.t3;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ResponseFormatJsonObject;
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

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * HOBBIES SEARCHER:
 * Searches users by hobbies and provides their full info:
 * Input: I need to gather people that love to go to mountains
 * Output:
 *    rock climbing: [{full user info},...],
 *    hiking: [{full user info},...],
 *    camping: [{full user info},...]
 */
public class InOutGroundingApp {

    private static final String SYSTEM_PROMPT = """
            You are a RAG-powered assistant that groups users by their hobbies.

            ## Flow:
            Step 1: User will ask to search users by their hobbies etc.
            Step 2: Will be performed search in the Vector store to find most relevant users.
            Step 3: You will be provided with CONTEXT (most relevant users, there will be user ID and information about
                    user), and with USER QUESTION.
            Step 4: You group by hobby users that have such hobby and return response according to Response Format

            ## Response Format (always return valid JSON):
            {
              "grouping_results": [
                {"hobby": "hiking", "user_ids": [1, 2, 3]},
                {"hobby": "camping", "user_ids": [4, 5]}
              ]
            }
            """;

    private static final String USER_PROMPT = """
            ## CONTEXT:
            {context}

            ## USER QUESTION:\s
            {query}""";

    private final OpenAIClient openAiClient;
    private final UserService userService;
    private final SimpleVectorStore vectorStore;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Set<String> knownUserIds = new HashSet<>();

    public InOutGroundingApp(OpenAiEmbeddingModel embeddingModel) {
        this.openAiClient = OpenAIOkHttpClient.builder()
                .apiKey(Constants.OPENAI_API_KEY)
                .build();
        this.userService = new UserService();
        this.vectorStore = SimpleVectorStore.builder(embeddingModel).build();
        initializeVectorStore();
    }

    private void initializeVectorStore() {
        System.out.println("🔍 Loading all users for initial vectorstore...");
        List<User> users = userService.getAllUsers();

        List<Document> documents = users.stream()
                .map(u -> Document.builder()
                        .id(String.valueOf(u.id()))
                        .text(u.toHobbyDocument())
                        .build())
                .collect(Collectors.toList());

        addInParallel(vectorStore, documents, 50);
        users.forEach(u -> knownUserIds.add(String.valueOf(u.id())));
        System.out.println("Setup FINISHED");
    }

    private void updateVectorStore() {
        List<User> currentUsers = userService.getAllUsers();
        Map<String, User> currentUsersMap = currentUsers.stream()
                .collect(Collectors.toMap(u -> String.valueOf(u.id()), u -> u));
        Set<String> currentIds = currentUsersMap.keySet();

        Set<String> newIds = new HashSet<>(currentIds);
        newIds.removeAll(knownUserIds);

        Set<String> deletedIds = new HashSet<>(knownUserIds);
        deletedIds.removeAll(currentIds);

        if (!deletedIds.isEmpty()) {
            vectorStore.delete(new ArrayList<>(deletedIds));
            knownUserIds.removeAll(deletedIds);
            System.out.println("Deleted " + deletedIds.size() + " users from vectorstore");
        }

        if (!newIds.isEmpty()) {
            List<Document> newDocuments = newIds.stream()
                    .map(id -> Document.builder()
                            .id(id)
                            .text(currentUsersMap.get(id).toHobbyDocument())
                            .build())
                    .collect(Collectors.toList());
            addInParallel(vectorStore, newDocuments, 50);
            knownUserIds.addAll(newIds);
            System.out.println("Added " + newIds.size() + " new users to vectorstore");
        }
    }

    private String retrieveContext(String query, int k, double minScore) {
        updateVectorStore();

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

    private List<GroupingResult> generateGroupingResults(String augmentedPrompt) {
        var params = ChatCompletionCreateParams.builder()
                .model(Constants.GPT_4_1_NANO)
                .temperature(0.0)
                .addSystemMessage(SYSTEM_PROMPT)
                .addUserMessage(augmentedPrompt)
                .responseFormat(ResponseFormatJsonObject.builder().build())
                .build();

        var completion = openAiClient.chat().completions().create(params);
        String responseJson = completion.choices().get(0).message().content().orElse("{}");

        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode groupings = root.path("grouping_results");

            if (groupings.isMissingNode() || !groupings.isArray()) {
                return List.of();
            }

            List<GroupingResult> results = new ArrayList<>();
            for (JsonNode grouping : groupings) {
                String hobby = grouping.path("hobby").asText();
                List<Integer> userIds = new ArrayList<>();
                grouping.path("user_ids").forEach(node -> userIds.add(node.asInt()));
                results.add(new GroupingResult(hobby, userIds));
            }
            return results;

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse grouping results: " + e.getMessage(), e);
        }
    }

    private void groundResponse(List<GroupingResult> groupingResults) {
        for (GroupingResult result : groupingResults) {
            System.out.println("Hobby: " + result.hobby());
            List<User> users = result.userIds().stream()
                    .map(userService::getUser)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toList());
            System.out.println("Users: " + users);
            System.out.println("----------");
        }
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

    private record GroupingResult(String hobby, List<Integer> userIds) {}

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

        InOutGroundingApp app = new InOutGroundingApp(embeddingModel);

        System.out.println("Query samples:");
        System.out.println(" - I need people who love to go to mountains");
        System.out.println(" - Find people who love to watch stars and night sky");
        System.out.println(" - I need people to go to fishing together");

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("\n> ");
            System.out.flush();
            if (!scanner.hasNextLine()) break;
            String query = scanner.nextLine().strip();
            if (query.isEmpty()) continue;
            if (query.equalsIgnoreCase("quit") || query.equalsIgnoreCase("exit")) break;

            // Input grounding: semantic search in vectorstore
            String context = app.retrieveContext(query, 100, 0.2);
            String augmented = app.augmentPrompt(query, context);
            // LLM structures result with user IDs grouped by hobby
            List<GroupingResult> groupingResults = app.generateGroupingResults(augmented);
            // Output grounding: fetch live user data for each found user
            app.groundResponse(groupingResults);
        }
    }
}
