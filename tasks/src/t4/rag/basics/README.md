# RAG (Retrieval-Augmented Generation) Implementation

A Java implementation task to build a complete RAG system for microwave manual assistance using Spring AI, LangChain4j, and OpenAI.

---

## Understanding the RAG Pipeline

Your implementation will demonstrate the complete RAG workflow:

1. **🔍 Retrieval**: Find relevant chunks from the microwave manual based on user query
2. **🔗 Augmentation**: Combine retrieved context with user question in a structured prompt
3. **🤖 Generation**: Use LLM to generate accurate answer based on the provided context

## Application diagram:

![](application-diagram.png)

---

## Task

### If the task in the main branch is hard for you, then switch to the `main-detailed` branch

Complete the **primary** implementation in [SpringRagApp.java](spring/SpringRagApp.java) by filling in all the TODO sections:

### ⚙️ **Step 1: Vector Store Setup (`setupVectorStore` method)**
- Print a startup message
- Build a `SimpleVectorStore` using `SimpleVectorStore.builder(embeddingModel).build()`
- Check if the index file already exists on disk
- If yes: load it using `store.load()` and print a confirmation
- If no: call `populateStore(store)`
- Return the store

### 📖 **Step 2: Document Processing (`populateStore` method)**
- Load [microwave_manual.txt](microwave_manual.txt) using `TextReader` and `FileSystemResource`
- Split documents into chunks using `TokenTextSplitter` (chunkSize=75)
- Add chunks to the store and save the index to disk

### 🔎 **Step 3: Context Retrieval (`retrieveContext` method)**
- Build a `SearchRequest` with query, topK, and similarityThreshold
- Call `vectorStore.similaritySearch(request)`
- Collect and return relevant document texts joined with `"\n\n"`

> You can experiment with these parameters in the `retrieveContext` call in `main`:

> `k`: Number of relevant chunks to retrieve

> `minScore`: Similarity threshold for chunk relevance (0.0–1.0)

### 🔗 **Step 4: Prompt Augmentation (`augmentPrompt` method)**
- Replace `{context}` and `{query}` placeholders in `USER_PROMPT_TEMPLATE`
- Print and return the formatted prompt

### 🤖 **Step 5: Answer Generation (`generateAnswer` method)**
- Build `ChatCompletionCreateParams` with system prompt, user message, model, and temperature
- Call the OpenAI client and extract the response content

### ▶️ **Step 6: Main Configuration**
- Instantiate `OpenAiEmbeddingModel` with `OpenAiApi`, `MetadataMode.EMBED`, and `OpenAiEmbeddingOptions` (model `text-embedding-3-small`)
- Wrap it in a `SpringRagApp` and start the interactive scanner loop

---

## Optional Task — LangChain4j variant

Complete the same RAG pipeline in [RagApp.java](langchain/RagApp.java).  
The logic is **identical** to the Spring version, but uses LangChain4j APIs instead:

| Concept | Spring AI | LangChain4j |
|---------|-----------|-------------|
| Vector store | `SimpleVectorStore` | `InMemoryEmbeddingStore<TextSegment>` |
| Document loading | `TextReader` + `FileSystemResource` | `FileSystemDocumentLoader.loadDocument()` |
| Splitting | `TokenTextSplitter` | `DocumentSplitters.recursive(300, 50)` |
| Embedding | `OpenAiEmbeddingModel` (Spring) | `OpenAiEmbeddingModel` (LangChain4j) |
| Search | `SearchRequest` + `similaritySearch()` | `EmbeddingSearchRequest` + `embeddingStore.search()` |
| Index persistence | `store.save()` / `store.load()` | `store.serializeToFile()` / `InMemoryEmbeddingStore.fromFile()` |

---

## Testing Your Implementation

### Valid request samples:
```
What is the maximum cooking time that can be set on microwave?
```
```
What are the steps to set the clock time on the microwave?
```
```
What is the ECO function on this microwave and how do you activate it?
```
```
What should you do if food in plastic or paper containers starts smoking during heating?
```
```
What is the recommended procedure for removing odors from the microwave oven?
```

### Invalid request samples:
```
What do you know about the DIALX Community?
```
```
What do you think about the dinosaur era? Why did they die?
```
