package t4.rag.basics.spring;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import commons.Constants;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.core.io.FileSystemResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class SpringRagApp {

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

    private static final String MANUAL_PATH = "tasks/src/t4/rag/basics/microwave_manual.txt";
    private static final Path INDEX_PATH = Paths.get("tasks/src/t4/rag/basics/spring/microwave_index.json");

    private final OpenAiEmbeddingModel embeddingModel;
    private final OpenAIClient openAiClient;
    private final SimpleVectorStore vectorStore;

    private SpringRagApp(OpenAiEmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
        this.openAiClient = OpenAIOkHttpClient.builder()
                .apiKey(Constants.OPENAI_API_KEY)
                .build();
        this.vectorStore = setupVectorStore();
    }

    private SimpleVectorStore setupVectorStore() {
        System.out.println("🔄 Initializing Microwave Manual RAG System...");

        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();

        if (Files.exists(INDEX_PATH)) {
            store.load(INDEX_PATH.toFile());
            System.out.println("✅ Loaded existing index from disk");
        } else {
            populateStore(store);
        }

        return store;
    }

    private void populateStore(SimpleVectorStore store) {
        System.out.println("📖 Loading text document...");
        List<Document> documents = new TextReader(new FileSystemResource(MANUAL_PATH)).get();

        System.out.println("✂️ Splitting document into chunks...");
        // ~75 tokens ≈ 300 chars to match the original chunk size
        List<Document> chunks = TokenTextSplitter.builder()
                .withChunkSize(75)
                .withMinChunkSizeChars(50)
                .withMinChunkLengthToEmbed(5)
                .withMaxNumChunks(10000)
                .withKeepSeparator(true)
                .build()
                .split(documents);
        System.out.printf("✅ Created %d chunks%n", chunks.size());

        System.out.println("🔍 Creating embeddings and index...");
        store.add(chunks);
        store.save(INDEX_PATH.toFile());
        System.out.println("💾 Index saved for future use");
        System.out.println("✅ RAG system initialized successfully!");
    }

    private String retrieveContext(String query, int k, double minScore) {
        System.out.printf("%s%n🔍 STEP 1: RETRIEVAL%n%s%n", "=".repeat(100), "-".repeat(100));
        System.out.println("Query: '" + query + "'");
        System.out.println("Searching for top " + k + " most relevant chunks with similarity score " + minScore + ":");

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(k)
                .similarityThreshold(minScore)
                .build();

        List<Document> results = vectorStore.similaritySearch(request);

        List<String> contextParts = results.stream()
                .map(doc -> {
                    String content = doc.getText();
                    Double score = doc.getScore();
                    if (score != null) {
                        System.out.printf("%n--- (Relevance Score: %.3f) ---%n", score);
                    } else {
                        System.out.println("\n---");
                    }
                    System.out.println("Content: " + content);
                    return content;
                })
                .collect(Collectors.toList());

        System.out.println("=".repeat(100));
        return String.join("\n\n", contextParts);
    }

    private String augmentPrompt(String query, String context) {
        System.out.printf("%n🔗 STEP 2: AUGMENTATION%n%s%n", "-".repeat(100));

        String augmented = USER_PROMPT_TEMPLATE
                .replace("{context}", context)
                .replace("{query}", query);

        System.out.println(augmented + "\n" + "=".repeat(100));
        return augmented;
    }

    private String generateAnswer(String augmentedPrompt) {
        System.out.printf("%n🤖 STEP 3: GENERATION%n%s%n", "-".repeat(100));

        var params = ChatCompletionCreateParams.builder()
                .model(Constants.GPT_5_4)
                .temperature(0.0)
                .addSystemMessage(SYSTEM_PROMPT)
                .addUserMessage(augmentedPrompt)
                .build();

        var completion = openAiClient.chat().completions().create(params);
        String answer = completion.choices().get(0).message().content()
                .orElseThrow(() -> new RuntimeException("No content in response"));

        System.out.println(answer + "\n" + "=".repeat(100));
        return answer;
    }

    public static void main(String[] args) {

        SpringRagApp rag = new SpringRagApp(
                new OpenAiEmbeddingModel(
                        OpenAiApi.builder()
                                .apiKey(Constants.OPENAI_API_KEY)
                                .build(),
                        MetadataMode.EMBED,
                        OpenAiEmbeddingOptions.builder()
                                .model("text-embedding-3-small")
                                .build()
                )
        );

        System.out.println("🎯 Microwave RAG Assistant (Spring AI)");

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("\n> ");
            System.out.flush();
            if (!scanner.hasNextLine()) break;
            String query = scanner.nextLine().strip();
            if (query.isEmpty()) continue;

            // Step 1: Retrieval
            String context = rag.retrieveContext(query, 4, 0.3); // play with k and minScore params
            // Step 2: Augmentation
            String augmented = rag.augmentPrompt(query, context);
            // Step 3: Generation
            rag.generateAnswer(augmented);
        }
    }
}
