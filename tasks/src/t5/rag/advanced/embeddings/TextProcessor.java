package t5.rag.advanced.embeddings;

import t5.rag.advanced.utils.TextUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TextProcessor {

    private final EmbeddingsClient embeddingsClient;
    private final String jdbcUrl;
    private final String dbUser;
    private final String dbPassword;

    public TextProcessor(EmbeddingsClient embeddingsClient, String host, int port, String database, String user, String password) {
        this.embeddingsClient = embeddingsClient;
        this.jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + database;
        this.dbUser = user;
        this.dbPassword = password;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
    }

    /**
     * Load file content, chunk it, generate embeddings, and store all chunks in the DB.
     * Truncates the vectors table before loading by default.
     */
    public void processTextFile(String fileName, int chunkSize, int overlap, int dimensions) {
        processTextFile(fileName, chunkSize, overlap, dimensions, true);
    }

    public void processTextFile(String fileName, int chunkSize, int overlap, int dimensions, boolean truncateTable) {
        if (chunkSize < 10) throw new IllegalArgumentException("chunk_size must be at least 10");
        if (overlap < 0) throw new IllegalArgumentException("overlap must be at least 0");
        if (overlap >= chunkSize) throw new IllegalArgumentException("overlap should be lower than chunkSize");

        if (truncateTable) {
            truncateTable();
        }

        try {
            String content = Files.readString(Path.of(fileName));
            List<String> chunks = TextUtils.chunkText(content, chunkSize, overlap);
            Map<Integer, List<Float>> embeddings = embeddingsClient.getEmbeddings(chunks, dimensions);

            System.out.println("Processing document: " + fileName);
            System.out.println("Total chunks: " + chunks.size());
            System.out.println("Total embeddings: " + embeddings.size());

            for (int i = 0; i < chunks.size(); i++) {
                saveChunk(embeddings.get(i), chunks.get(i), fileName);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void truncateTable() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE vectors");
            System.out.println("Table has been successfully truncated.");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveChunk(List<Float> embedding, String chunk, String documentName) {
        String vectorString = "[" + embedding.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")) + "]";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO vectors (document_name, text, embedding) VALUES (?, ?, ?::vector)")) {
            ps.setString(1, documentName);
            ps.setString(2, chunk);
            ps.setString(3, vectorString);
            ps.executeUpdate();
            System.out.println("Stored chunk from document: " + documentName);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Perform similarity search against stored vectors.
     *
     * @param searchMode     Euclidean or Cosine distance
     * @param userRequest    Query text to embed and search with
     * @param topK           Maximum number of results to return
     * @param scoreThreshold Minimum similarity score [0.0, 1.0]
     * @param dimensions     Must match the dimensions used during indexing
     */
    public List<String> search(SearchMode searchMode, String userRequest, int topK, double scoreThreshold, int dimensions) {
        if (topK < 1) throw new IllegalArgumentException("top_k must be at least 1");
        if (scoreThreshold < 0 || scoreThreshold > 1) throw new IllegalArgumentException("score_threshold must be in [0.0..., 0.99...] range");

        List<Float> queryEmbedding = embeddingsClient.getEmbeddings(userRequest, dimensions).get(0);
        String vectorString = "[" + queryEmbedding.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")) + "]";

        double maxDistance;
        if (searchMode == SearchMode.COSINE_DISTANCE) {
            maxDistance = 1.0 - scoreThreshold;
        } else {
            // Euclidean: score = 1/(1+distance) → distance = (1/score) - 1
            maxDistance = scoreThreshold == 0 ? Double.MAX_VALUE : (1.0 / scoreThreshold) - 1.0;
        }

        List<String> retrievedChunks = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(buildSearchQuery(searchMode))) {
            ps.setString(1, vectorString);
            ps.setString(2, vectorString);
            ps.setDouble(3, maxDistance);
            ps.setInt(4, topK);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String text = rs.getString("text");
                    double distance = rs.getDouble("distance");

                    double similarity = searchMode == SearchMode.COSINE_DISTANCE
                            ? 1.0 - distance
                            : 1.0 / (1.0 + distance);

                    System.out.printf("---Similarity score: %.2f---%n", similarity);
                    System.out.println("Data: " + text + "\n");
                    retrievedChunks.add(text);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return retrievedChunks;
    }

    private String buildSearchQuery(SearchMode searchMode) {
        String operator = searchMode == SearchMode.EUCLIDEAN_DISTANCE ? "<->" : "<=>";
        return "SELECT text, embedding " + operator + " ?::vector AS distance " +
               "FROM vectors " +
               "WHERE embedding " + operator + " ?::vector <= ? " +
               "ORDER BY distance " +
               "LIMIT ?";
    }
}
