package t5.rag.advanced.embeddings;

public enum SearchMode {
    EUCLIDEAN_DISTANCE("euclidean"),
    COSINE_DISTANCE("cosine");

    private final String value;

    SearchMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
