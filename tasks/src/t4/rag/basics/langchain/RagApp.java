package t4.rag.basics.langchain;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import commons.Constants;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class RagApp {

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
    private static final Path INDEX_PATH = Paths.get("tasks/src/t4/rag/basics/langchain/microwave_index.json");

    private final EmbeddingModel embeddingModel;
    private final OpenAIClient openAiClient;
    private final EmbeddingStore<TextSegment> embeddingStore;

    private RagApp(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
        this.openAiClient = OpenAIOkHttpClient.builder()
                .apiKey(Constants.OPENAI_API_KEY)
                .build();
        this.embeddingStore = setupEmbeddingStore();
    }

    private EmbeddingStore<TextSegment> setupEmbeddingStore() {
        System.out.println("🔄 Initializing Microwave Manual RAG System...");

        if (Files.exists(INDEX_PATH)) {
            System.out.println("✅ Loaded existing index from disk");
            return InMemoryEmbeddingStore.fromFile(INDEX_PATH);
        } else {
            return createNewIndex();
        }
    }

    private InMemoryEmbeddingStore<TextSegment> createNewIndex() {
        System.out.println("📖 Loading text document...");
        Document document = FileSystemDocumentLoader.loadDocument(Paths.get(MANUAL_PATH));

        System.out.println("✂️ Splitting document into chunks...");
        List<TextSegment> segments = DocumentSplitters.recursive(300, 50).split(document);
        System.out.printf("✅ Created %d chunks%n", segments.size());

        System.out.println("🔍 Creating embeddings and index...");
        InMemoryEmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        store.addAll(embeddings, segments);
        store.serializeToFile(INDEX_PATH);
        System.out.println("💾 Index saved for future use");
        System.out.println("✅ RAG system initialized successfully!");

        return store;
    }

    private String retrieveContext(String query, int k, double minScore) {
        System.out.printf("%s%n🔍 STEP 1: RETRIEVAL%n%s%n", "=".repeat(100), "-".repeat(100));
        System.out.println("Query: '" + query + "'");
        System.out.println("Searching for top " + k + " most relevant chunks with similarity score " + minScore + ":");

        Embedding queryEmbedding = embeddingModel.embed(query).content();

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(k)
                .minScore(minScore)
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);

        List<String> contextParts = new ArrayList<>();
        for (EmbeddingMatch<TextSegment> match : result.matches()) {
            String content = match.embedded().text();
            contextParts.add(content);
            System.out.printf("%n--- (Relevance Score: %.3f) ---%n", match.score());
            System.out.println("Content: " + content);
        }

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
        RagApp rag = new RagApp(
                OpenAiEmbeddingModel.builder()
                        .apiKey(Constants.OPENAI_API_KEY)
                        .modelName("text-embedding-3-small")
                        .build()
        );

        System.out.println("🎯 Microwave RAG Assistant");

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("\n> ");
            System.out.flush();
            if (!scanner.hasNextLine()) break;
            String query = scanner.nextLine().strip();
            if (query.isEmpty()) continue;

            // Step 1: Retrieval
            String context = rag.retrieveContext(query, 4, 0.7); // play with k and minScore params
            // Step 2: Augmentation
            String augmented = rag.augmentPrompt(query, context);
            // Step 3: Generation
            rag.generateAnswer(augmented);
        }
    }
}
