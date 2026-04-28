package t12.skills.task.custom;

import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import commons.Constants;
import t12.skills.task.custom.mcp.McpClient;
import t12.skills.task.custom.tools.BaseTool;
import t12.skills.task.custom.tools.PythonCodeInterpreter;
import t12.skills.task.custom.tools.ReadSkill;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Custom skills agent — equivalent of custom_app.py.
 *
 * Prerequisites:
 *   docker-compose up   (starts the Python code-interpreter MCP server on :8050)
 *
 * Run from the project root so that the relative SKILLS_DIR path resolves correctly.
 */
public class App {

    private static final Path SKILLS_DIR = Path.of("tasks/src/t12/skills/task/custom/_skills");
    private static final String MCP_URL = "http://localhost:8050/mcp/";
    private static final String MCP_TOOL_NAME = "execute_code";

    public static void main(String[] args) throws Exception {
        List<SkillMetadata> skills = SkillLoader.loadSkills(SKILLS_DIR);
        if (skills.isEmpty()) {
            System.err.println("ERROR: no valid skills found in " + SKILLS_DIR.toAbsolutePath());
            return;
        }
        System.out.println("Loaded " + skills.size() + " skill(s): "
                + skills.stream().map(SkillMetadata::name).toList());

        String systemPrompt = buildSystemPrompt(skills, MCP_TOOL_NAME);
        System.out.println("\n📄 System prompt:\n" + systemPrompt + "\n");

        try (McpClient mcpClient = new McpClient(MCP_URL)) {
            mcpClient.connect();

            List<BaseTool> tools = List.of(
                    new ReadSkill(SKILLS_DIR),
                    PythonCodeInterpreter.create(mcpClient, SKILLS_DIR, MCP_TOOL_NAME)
            );

            Agent agent = new Agent(Constants.OPENAI_API_KEY, Constants.GPT_5_4, tools);

            List<ChatCompletionMessageParam> messages = new ArrayList<>();
            messages.add(ChatCompletionMessageParam.ofSystem(
                    ChatCompletionSystemMessageParam.builder().content(systemPrompt).build()
            ));

            Scanner scanner = new Scanner(System.in);
            System.out.println("Agent is ready. Type your query or 'exit' to quit.\n");

            while (true) {
                System.out.print("➡️: ");
                String userInput = scanner.nextLine().strip();

                if ("exit".equalsIgnoreCase(userInput)) {
                    System.out.println("Exiting. Goodbye!");
                    break;
                }

                messages.add(ChatCompletionMessageParam.ofUser(
                        ChatCompletionUserMessageParam.builder().content(userInput).build()
                ));

                ChatCompletionMessageParam aiMessage = agent.getCompletion(messages);
                messages.add(aiMessage);
            }
        }
    }

    private static String buildSystemPrompt(List<SkillMetadata> skills, String mcpToolName) {
        StringBuilder xml = new StringBuilder("<available_skills>\n");
        for (SkillMetadata skill : skills) {
            xml.append("  <skill name=\"").append(skill.name()).append("\">\n");
            xml.append("    <description>").append(skill.description()).append("</description>\n");
            if (skill.license() != null) {
                xml.append("    <license>").append(skill.license()).append("</license>\n");
            }
            if (skill.compatibility() != null) {
                xml.append("    <compatibility>").append(skill.compatibility()).append("</compatibility>\n");
            }
            if (skill.metadata() != null) {
                xml.append("    <metadata>\n");
                skill.metadata().forEach((k, v) ->
                        xml.append("      <").append(k).append(">")
                                .append(v)
                                .append("</").append(k).append(">\n"));
                xml.append("    </metadata>\n");
            }
            if (skill.allowedTools() != null) {
                xml.append("    <allowed-tools>")
                        .append(String.join(" ", skill.allowedTools()))
                        .append("</allowed-tools>\n");
            }
            xml.append("  </skill>\n");
        }
        xml.append("</available_skills>");

        return """
                You are an AI assistant with access to agent skills.

                %s

                ## How to use skills

                When the user's request matches a skill, activate it:
                1. Call `read_skill` with the skill's SKILL.md path (e.g. path="/<skill-name>/SKILL.md") to load
                   its full instructions.
                2. Follow the instructions in the loaded SKILL.md precisely.
                3. If the instructions reference additional files (scripts, references, assets), read them on demand
                   using `read_skill` (e.g. path="/<skill-name>/scripts/calculate.py").
                4. If the skill requires running a Python script, execute it with `%s`.

                Always read the relevant SKILL.md before performing the task."""
                .formatted(xml, mcpToolName);
    }
}
