package t12.skills.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonValue;
import com.openai.models.responses.EasyInputMessage;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseInputItem;
import commons.Constants;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * OpenAI native skills agent — equivalent of openai_app.py.
 *
 * Skills management (list/create/delete) uses raw HTTP because the OpenAI Java
 * SDK 2.1.0 does not yet expose the Skills API. The chat loop uses the SDK's
 * Responses API (client.responses().create()), with the shell tool injected via
 * putAdditionalBodyProperty because the shell+environment type is also absent
 * from the SDK's typed Tool union.  previous_response_id chains turns
 * server-side so the full history does not need to be resent on every turn.
 */
public class OpenAiApp {

    private static final String OPENAI_API = "https://api.openai.com/v1";
    private static final Path BASE_SKILLS_DIR = Path.of("tasks/src/t12/skills/_skills");

    private static final String STYLE_SKILL_NAME = "style-guide";
    private static final Path STYLE_SKILL_DIR = BASE_SKILLS_DIR.resolve(STYLE_SKILL_NAME);

    private static final String CALCULATOR_SKILL_NAME = "calculator";
    private static final Path CALCULATOR_SKILL_DIR = BASE_SKILLS_DIR.resolve(CALCULATOR_SKILL_NAME);

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String apiKey = Constants.OPENAI_API_KEY;

    private final OpenAIClient openAIClient;

    public OpenAiApp() {
        this.openAIClient = OpenAIOkHttpClient.builder()
                .apiKey(Constants.OPENAI_API_KEY)
                .build();
    }

    public static void main(String[] args) throws Exception {
        new OpenAiApp().run();
    }

    private void run() throws Exception {
        String skillId = getOrCreateSkill(CALCULATOR_SKILL_NAME, CALCULATOR_SKILL_DIR);
        chat(skillId);
        deleteSkills();
    }

    // ── Skill management (raw HTTP — Skills API not yet in Java SDK) ─────────

    private String getOrCreateSkill(String skillName, Path skillDir) throws Exception {
        List<Map<String, Object>> existing = listSkills();

        System.out.println("\n--- LIST SKILLS RESPONSE ---");
        System.out.println(pretty(existing));
        System.out.println("----------------------------\n");

        for (Map<String, Object> skill : existing) {
            if (skillName.equals(skill.get("name"))) {
                String id = (String) skill.get("id");
                System.out.println("Skill already exists: " + id);
                return id;
            }
        }

        return createSkill(skillName, skillDir);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listSkills() throws Exception {
        HttpRequest request = openaiRequest("GET", "/skills", null);
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        requireOk(response);
        JsonNode body = mapper.readTree(response.body());
        List<Map<String, Object>> skills = new ArrayList<>();
        body.path("data").forEach(node -> skills.add(mapper.convertValue(node, Map.class)));
        return skills;
    }

    private String createSkill(String skillName, Path skillDir) throws Exception {
        byte[] zipBytes = zipSkill(skillDir);
        String boundary = "----FormBoundary" + UUID.randomUUID().toString().replace("-", "");
        byte[] multipartBody = buildMultipartBody(boundary, skillDir.getFileName().toString(), zipBytes);

        System.out.println("\n--- CREATE SKILL REQUEST ---");
        System.out.println(pretty(Map.of(
                "name", skillName,
                "skill_dir", skillDir.toString(),
                "zip_size_bytes", zipBytes.length
        )));
        System.out.println("----------------------------\n");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_API + "/skills"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        requireOk(response);

        System.out.println("\n--- CREATE SKILL RESPONSE ---");
        System.out.println(pretty(mapper.readTree(response.body())));
        System.out.println("-----------------------------\n");

        String id = mapper.readTree(response.body()).path("id").asText();
        System.out.println("Skill uploaded: " + id);
        return id;
    }

    private void deleteSkills() throws Exception {
        for (Map<String, Object> skill : listSkills()) {
            String skillId = (String) skill.get("id");
            String name = (String) skill.get("name");
            HttpRequest request = openaiRequest("DELETE", "/skills/" + skillId, null);
            http.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Deleted skill " + name);
        }
    }

    // ── Chat loop (OpenAI SDK Responses API) ─────────────────────────────────

    private void chat(String skillId) throws Exception {
        String previousResponseId = null;

        System.out.println("\nAgent is ready. Type your query or 'exit' to quit.\n");

        var scanner = new Scanner(System.in);
        while (true) {
            System.out.print("You: ");
            String userInput = scanner.nextLine().strip();
            if ("exit".equalsIgnoreCase(userInput)) break;

            // Build environment — reuse container on subsequent turns to preserve state
            Map<String, Object> environment = Map.of(
                    "type", "container_auto",
                    "skills", List.of(Map.of("type", "skill_reference", "skill_id", skillId))
            );
            Map<String, Object> shellTool = Map.of("type", "shell", "environment", environment);

            // Pretty-print logical request payload
            Map<String, Object> requestPayload = new HashMap<>();
            requestPayload.put("model", Constants.GPT_5_4);
            requestPayload.put("input", List.of(Map.of("role", "user", "content", userInput)));
            requestPayload.put("tools", List.of(shellTool));
            if (previousResponseId != null) {
                requestPayload.put("previous_response_id", previousResponseId);
            }

            System.out.println("\n--- REQUEST ---");
            System.out.println(pretty(requestPayload));
            System.out.println("---------------\n");

            // Build the SDK request — inject shell tool via additional body property
            // because the shell+environment type is not in the typed Tool union yet.
            var paramsBuilder = ResponseCreateParams.builder()
                    .model(Constants.GPT_5_4)
                    .inputOfResponse(List.of(
                            ResponseInputItem.ofEasyInputMessage(
                                    EasyInputMessage.builder()
                                            .role(EasyInputMessage.Role.USER)
                                            .content(userInput)
                                            .build()
                            )
                    ))
                    .putAdditionalBodyProperty("tools", JsonValue.from(List.of(shellTool)));

            if (previousResponseId != null) {
                paramsBuilder.previousResponseId(previousResponseId);
            }

            Response response = openAIClient.responses().create(paramsBuilder.build());
            previousResponseId = response.id();

            System.out.println("\n--- RESPONSE ---");
            System.out.println(pretty(response));
            System.out.println("----------------\n");
        }
    }

    // ── HTTP helpers (for Skills API) ─────────────────────────────────────────

    private HttpRequest openaiRequest(String method, String path, String jsonBody) {
        var builder = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_API + path))
                .header("Authorization", "Bearer " + apiKey);

        if (jsonBody != null) {
            builder.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(jsonBody));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }
        return builder.build();
    }

    private void requireOk(HttpResponse<String> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        }
    }

    /**
     * Zips all files under skillDir, preserving paths relative to skillDir's parent.
     * Mirrors the zip_skill() function in openai_app.py.
     */
    private byte[] zipSkill(Path skillDir) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(buf)) {
            try (Stream<Path> paths = Files.walk(skillDir)) {
                for (Path file : paths.filter(Files::isRegularFile).toList()) {
                    String entryName = skillDir.getParent().relativize(file).toString().replace('\\', '/');
                    zip.putNextEntry(new ZipEntry(entryName));
                    zip.write(Files.readAllBytes(file));
                    zip.closeEntry();
                }
            }
        }
        return buf.toByteArray();
    }

    /**
     * Builds a multipart/form-data body with a single file part for the zip.
     */
    private byte[] buildMultipartBody(String boundary, String zipFileName, byte[] zipBytes) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String CRLF = "\r\n";
        String dash = "--";

        out.write((dash + boundary + CRLF).getBytes());
        out.write(("Content-Disposition: form-data; name=\"files\"; filename=\"" + zipFileName + ".zip\"" + CRLF).getBytes());
        out.write(("Content-Type: application/zip" + CRLF + CRLF).getBytes());
        out.write(zipBytes);
        out.write(CRLF.getBytes());

        out.write((dash + boundary + dash + CRLF).getBytes());
        return out.toByteArray();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String pretty(Object obj) throws Exception {
        if (obj instanceof String s) return s;
        return mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(mapper.readValue(mapper.writeValueAsString(obj), Object.class));
    }
}
