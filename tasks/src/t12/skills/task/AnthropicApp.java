package t12.skills.task;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.beta.AnthropicBeta;
import com.anthropic.models.beta.messages.BetaCodeExecutionTool20250825;
import com.anthropic.models.beta.messages.BetaContainerParams;
import com.anthropic.models.beta.messages.BetaMessage;
import com.anthropic.models.beta.messages.BetaMessageParam;
import com.anthropic.models.beta.messages.BetaSkillParams;
import com.anthropic.models.beta.messages.MessageCreateParams;
import com.anthropic.models.beta.skills.SkillCreateParams;
import com.anthropic.models.beta.skills.SkillCreateResponse;
import com.anthropic.models.beta.skills.SkillDeleteParams;
import com.anthropic.models.beta.skills.SkillListParams;
import com.anthropic.models.beta.skills.SkillListResponse;
import com.anthropic.models.beta.skills.versions.VersionDeleteParams;
import com.anthropic.models.beta.skills.versions.VersionListParams;
import com.anthropic.models.beta.skills.versions.VersionListResponse;
import com.anthropic.models.messages.Model;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import commons.Constants;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Anthropic beta skills agent — equivalent of anthropic_app.py.
 *
 * Uses the Anthropic SDK (AnthropicClient) to upload a skill directory, then
 * runs a multi-turn chat loop that reuses the container across turns so the
 * code execution environment persists session state.
 *
 * Beta headers required:
 *   anthropic-beta: code-execution-2025-08-25, skills-2025-10-02
 */
public class AnthropicApp {

    private static final String SKILLS_VERSION = "skills-2025-10-02";
    private static final String CODE_EXEC_VERSION = "code-execution-2025-08-25";

    private static final Path BASE_SKILLS_DIR = Path.of("tasks/src/t12/skills/_skills");
    private static final String STYLE_SKILL_TITLE = "style-guide";
    private static final String CALCULATOR_SKILL_TITLE = "calculator";

    private final AnthropicClient client;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public AnthropicApp() {
        this.client = AnthropicOkHttpClient.builder()
                .apiKey(Constants.ANTHROPIC_API_KEY)
                .build();
    }

    public static void main(String[] args) throws Exception {
        new AnthropicApp().run();
    }

    private void run() throws Exception {
        Path skillDir = BASE_SKILLS_DIR.resolve(STYLE_SKILL_TITLE);
        String skillId = getOrCreateSkill(STYLE_SKILL_TITLE, skillDir);
        chat(skillId);
        deleteSkills();
    }

    // ── Skill management ────────────────────────────────────────────────────

    private String getOrCreateSkill(String title, Path skillDir) throws Exception {
        var listParams = SkillListParams.builder()
                .source("custom")
                .addBeta(AnthropicBeta.of(SKILLS_VERSION))
                .build();

        List<SkillListResponse> skills = client.beta().skills().list(listParams).data();

        System.out.println("\n--- LIST SKILLS RESPONSE ---");
        System.out.println(pretty(skills));
        System.out.println("----------------------------\n");

        for (SkillListResponse skill : skills) {
            if (title.equals(skill.displayTitle().orElse(null))) {
                System.out.println("Skill already exists: " + skill.id()
                        + " (latest version: " + skill.latestVersion().orElse("?") + ")");
                return skill.id();
            }
        }

        return createSkill(title, skillDir);
    }

    private String createSkill(String title, Path skillDir) throws Exception {
        var paramsBuilder = SkillCreateParams.builder()
                .displayTitle(title)
                .addBeta(AnthropicBeta.of(SKILLS_VERSION));

        try (Stream<Path> paths = Files.walk(skillDir)) {
            paths.filter(Files::isRegularFile).forEach(paramsBuilder::addFile);
        }

        System.out.println("\n--- CREATE SKILL REQUEST ---");
        System.out.println(pretty(Map.of("display_title", title, "skill_dir", skillDir.toString())));
        System.out.println("----------------------------\n");

        SkillCreateResponse response = client.beta().skills().create(paramsBuilder.build());

        System.out.println("\n--- CREATE SKILL RESPONSE ---");
        System.out.println(pretty(response));
        System.out.println("-----------------------------\n");

        System.out.println("Skill uploaded: " + response.id());
        return response.id();
    }

    private void deleteSkills() throws Exception {
        var listParams = SkillListParams.builder()
                .source("custom")
                .addBeta(AnthropicBeta.of(SKILLS_VERSION))
                .build();

        for (SkillListResponse skill : client.beta().skills().list(listParams).data()) {
            String skillId = skill.id();
            String title = skill.displayTitle().orElse("unknown");

            List<VersionListResponse> versions = client.beta().skills().versions().list(
                    VersionListParams.builder()
                            .skillId(skillId)
                            .addBeta(AnthropicBeta.of(SKILLS_VERSION))
                            .build()
            ).data();

            for (VersionListResponse version : versions) {
                client.beta().skills().versions().delete(
                        VersionDeleteParams.builder()
                                .skillId(skillId)
                                .version(version.version())
                                .addBeta(AnthropicBeta.of(SKILLS_VERSION))
                                .build());
                System.out.println("Deleted version " + version.version() + " of " + title);
            }

            client.beta().skills().delete(
                    SkillDeleteParams.builder()
                            .skillId(skillId)
                            .addBeta(AnthropicBeta.of(SKILLS_VERSION))
                            .build());
            System.out.println("Deleted skill " + title);
        }
    }

    // ── Chat loop ────────────────────────────────────────────────────────────

    private void chat(String skillId) throws Exception {
        List<Map<String, Object>> messages = new ArrayList<>();
        String containerId = null;

        System.out.println("\nStyle Guide Agent is ready. Ask it to write, rewrite, or review any text.");
        System.out.println("Type 'exit' to quit.\n");

        var scanner = new java.util.Scanner(System.in);
        while (true) {
            System.out.print("You: ");
            String userInput = scanner.nextLine().strip();
            if ("exit".equalsIgnoreCase(userInput)) break;

            messages.add(Map.of("role", "user", "content", userInput));

            // Pretty-print the logical request payload
            Map<String, Object> containerMap = new HashMap<>();
            containerMap.put("skills", List.of(Map.of(
                    "type", "custom", "skill_id", skillId, "version", "latest")));
            if (containerId != null) containerMap.put("id", containerId);

            Map<String, Object> requestPayload = Map.of(
                    "model", Constants.CLAUDE_SONNET_4_5,
                    "max_tokens", 4096,
                    "messages", messages,
                    "container", containerMap,
                    "betas", List.of(CODE_EXEC_VERSION, SKILLS_VERSION),
                    "tools", List.of(Map.of("type", "code_execution_20250825", "name", "code_execution"))
            );

            System.out.println("\n--- REQUEST ---");
            System.out.println(pretty(requestPayload));
            System.out.println("---------------\n");

            // Build the typed SDK container params
            var containerBuilder = BetaContainerParams.builder()
                    .addSkill(BetaSkillParams.builder()
                            .skillId(skillId)
                            .type(BetaSkillParams.Type.CUSTOM)
                            .version("latest")
                            .build());
            if (containerId != null) containerBuilder.id(containerId);

            // Convert accumulated messages (raw maps) to typed BetaMessageParam list via JSON
            List<BetaMessageParam> messageParams = mapper.readValue(
                    mapper.writeValueAsString(messages), new TypeReference<>() {});

            var createParams = MessageCreateParams.builder()
                    .model(Model.CLAUDE_SONNET_4_6)
                    .maxTokens(4096)
                    .messages(messageParams)
                    .container(containerBuilder.build())
                    .addTool(BetaCodeExecutionTool20250825.builder().build())
                    .addBeta(AnthropicBeta.of(CODE_EXEC_VERSION))
                    .addBeta(AnthropicBeta.of(SKILLS_VERSION))
                    .build();

            BetaMessage response = client.beta().messages().create(createParams);

            System.out.println("\n--- RESPONSE ---");
            System.out.println(pretty(response));
            System.out.println("----------------\n");

            // Persist container id for subsequent turns
            if (response.container().isPresent()) {
                containerId = response.container().get().id();
            }

            // Append assistant message as raw JSON so it round-trips correctly next turn
            Object responseContent = mapper.readValue(
                    mapper.writeValueAsString(response.content()), Object.class);
            messages.add(Map.of("role", "assistant", "content", responseContent));
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String pretty(Object obj) throws Exception {
        if (obj instanceof String s) return s;
        return mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(mapper.readValue(mapper.writeValueAsString(obj), Object.class));
    }
}
