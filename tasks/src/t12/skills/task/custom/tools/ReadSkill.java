package t12.skills.task.custom.tools;

import t12.skills.task.custom.FileUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class ReadSkill extends BaseTool {

    private final Path skillsDir;

    public ReadSkill(Path skillsDir) {
        this.skillsDir = skillsDir.toAbsolutePath().normalize();
    }

    @Override
    public String getName() {
        return "read_skill";
    }

    @Override
    public String getDescription() {
        return "Read a skill file by its path. Use this to access skill instructions, " +
               "scripts, references, or any other skill resource. " +
               "Paths are relative to the skills root, e.g. /calculator/SKILL.md " +
               "or /calculator/scripts/calculate.py";
    }

    @Override
    public Map<String, Object> getParameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "path", Map.of(
                                "type", "string",
                                "description", "Path to the skill file relative to the skills root. " +
                                               "E.g. /calculator/SKILL.md or /calculator/scripts/calculate.py"
                        )
                ),
                "required", List.of("path")
        );
    }

    @Override
    protected String doExecute(Map<String, Object> arguments) {
        String rawPath = ((String) arguments.get("path")).replaceFirst("^/+", "");
        Path fullPath = skillsDir.resolve(rawPath).normalize();
        return FileUtils.getFileContent(fullPath);
    }
}
