package t5.rag.advanced.utils;

import java.util.ArrayList;
import java.util.List;

public class TextUtils {

    /**
     * Split text into character-level chunks with overlap.
     * Example: text="Hello World", chunkSize=8, overlap=3
     *   Chunk 1: "Hello Wo" (0-7)
     *   Chunk 2: "o World " (5-12)
     *   ...
     */
    public static List<String> chunkText(String text, int chunkSize, int overlap) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        if (text.length() <= chunkSize) {
            return List.of(text);
        }

        List<String> chunks = new ArrayList<>();
        int currentPosition = 0;

        while (currentPosition < text.length()) {
            int endPosition = Math.min(currentPosition + chunkSize, text.length());
            chunks.add(text.substring(currentPosition, endPosition));
            currentPosition = endPosition - overlap;
            if (currentPosition >= text.length() - overlap && endPosition == text.length()) {
                break;
            }
        }

        return chunks;
    }
}
