package t6.grounding.t2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ResponseFormatJsonObject;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import commons.Constants;
import t6.grounding.User;
import t6.grounding.UserService;

import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class InputApiBasedGroundingApp {

    private static final String QUERY_ANALYSIS_PROMPT = """
            You are a query analysis system that extracts search parameters from user questions about users.

            ## Available Search Fields:
            - **name**: User's first name (e.g., "John", "Mary")
            - **surname**: User's last name (e.g., "Smith", "Johnson")
            - **email**: User's email address (e.g., "john@example.com")

            ## Instructions:
            1. Analyze the user's question and identify what they're looking for
            2. Extract specific search values mentioned in the query
            3. Map them to the appropriate search fields
            4. If multiple search criteria are mentioned, include all of them
            5. Only extract explicit values - don't infer or assume values not mentioned

            ## Examples:
            - "Who is John?" → name: "John"
            - "Find users with surname Smith" → surname: "Smith"
            - "Look for john@example.com" → email: "john@example.com"
            - "Find John Smith" → name: "John", surname: "Smith"
            - "I need user emails that filled with hiking" → No clear search parameters (return empty list)

            ## Response Format (always return valid JSON):
            {
              "search_request_parameters": [
                {"search_field": "name", "search_value": "John"},
                {"search_field": "surname", "search_value": "Smith"}
              ]
            }
            search_field must be one of: "name", "surname", "email"
            """;

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
            - Be conversational and helpful in your responses.
            - When presenting user information, format it clearly and include relevant details.
            """;

    private static final String USER_PROMPT = """
            ## RAG CONTEXT:
            {context}

            ## USER QUESTION:\s
            {query}""";

    private final OpenAIClient openAiClient;
    private final UserService userService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public InputApiBasedGroundingApp() {
        this.openAiClient = OpenAIOkHttpClient.builder()
                .apiKey(Constants.OPENAI_API_KEY)
                .build();
        this.userService = new UserService();
    }

    private List<User> retrieveContext(String userQuestion) {
        var params = ChatCompletionCreateParams.builder()
                .model(Constants.GPT_4_1_NANO)
                .temperature(0.0)
                .addSystemMessage(QUERY_ANALYSIS_PROMPT)
                .addUserMessage(userQuestion)
                .responseFormat(ResponseFormatJsonObject.builder().build())
                .build();

        var completion = openAiClient.chat().completions().create(params);
        String responseJson = completion.choices().get(0).message().content().orElse("{}");

        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode searchParams = root.path("search_request_parameters");

            if (searchParams.isMissingNode() || !searchParams.isArray() || searchParams.isEmpty()) {
                System.out.println("No specific search parameters found!");
                return List.of();
            }

            String name = null, surname = null, email = null;
            for (JsonNode param : searchParams) {
                String field = param.path("search_field").asText();
                String value = param.path("search_value").asText();
                switch (field) {
                    case "name" -> name = value;
                    case "surname" -> surname = value;
                    case "email" -> email = value;
                }
            }

            System.out.printf("Searching with parameters: name=%s, surname=%s, email=%s%n", name, surname, email);
            return userService.searchUsers(name, surname, email);

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse search parameters: " + e.getMessage(), e);
        }
    }

    private String augmentPrompt(String userQuestion, List<User> context) {
        String contextStr = context.stream()
                .map(User::toDocument)
                .collect(Collectors.joining("\n"));

        String augmented = USER_PROMPT
                .replace("{context}", contextStr)
                .replace("{query}", userQuestion);
        System.out.println(augmented);
        return augmented;
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
        InputApiBasedGroundingApp app = new InputApiBasedGroundingApp();

        System.out.println("Query samples:");
        System.out.println(" - I need user emails that filled with hiking and psychology");
        System.out.println(" - Who is John?");
        System.out.println(" - Find users with surname Adams");
        System.out.println(" - Do we have smbd with name John that love painting?");

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("\n> ");
            System.out.flush();
            if (!scanner.hasNextLine()) break;
            String userQuestion = scanner.nextLine().strip();
            if (userQuestion.isEmpty()) continue;
            if (userQuestion.equalsIgnoreCase("quit") || userQuestion.equalsIgnoreCase("exit")) break;

            System.out.println("\n--- Retrieving context ---");
            List<User> context = app.retrieveContext(userQuestion);

            if (!context.isEmpty()) {
                System.out.println("\n--- Augmenting prompt ---");
                String augmented = app.augmentPrompt(userQuestion, context);

                System.out.println("\n--- Generating answer ---");
                String answer = app.generateAnswer(augmented);
                System.out.println("\nAnswer: " + answer + "\n");
            } else {
                System.out.println("\n--- No relevant information found ---");
            }
        }
    }
}

// The problems with API based Grounding approach are:
//   - We need a Pre-Step to figure out what field should be used for search (Takes time)
//   - Values for search should be correct (✅ John -> ❌ Jonh)
//   - Is not so flexible
// Benefits are:
//   - We fetch actual data (new users added and deleted every 5 minutes)
//   - Costs reduce
