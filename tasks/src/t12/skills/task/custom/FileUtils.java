package t12.skills.task.custom;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUtils {

    public static String getFileContent(Path path) {
        if (!Files.exists(path)) {
            return "ERROR: File not found: " + path;
        }
        if (!Files.isRegularFile(path)) {
            return "ERROR: Not a file: " + path;
        }
        try {
            return Files.readString(path);
        } catch (IOException e) {
            return "ERROR: Could not read file: " + e.getMessage();
        }
    }
}
