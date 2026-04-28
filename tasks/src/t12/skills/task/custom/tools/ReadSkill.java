package t12.skills.task.custom.tools;

import commons.exceptions.TaskNotImplementedException;
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
        //TODO:
        // Return a String describing the tool: it reads a skill file by its path,
        // used to access skill instructions, scripts, references, or any other skill resource;
        // paths are relative to the skills root,
        // e.g. /calculator/SKILL.md or /calculator/scripts/calculate.py
        throw new TaskNotImplementedException();
    }

    @Override
    public Map<String, Object> getParameters() {
        //TODO:
        // Return Map.of(
        //   "type", "object",
        //   "properties", Map.of(
        //       "path", Map.of(
        //           "type", "string",
        //           "description", "Path to the skill file relative to the skills root. "
        //                          + "E.g. /calculator/SKILL.md or /calculator/scripts/calculate.py")),
        //   "required", List.of("path"))
        throw new TaskNotImplementedException();
    }

    @Override
    protected String doExecute(Map<String, Object> arguments) {
        //TODO:
        // 1. Strip leading "/" from arguments.get("path") with replaceFirst("^/+", ""), assign to `rawPath`
        // 2. Resolve full path: skillsDir.resolve(rawPath).normalize(), assign to `fullPath`
        // 3. Return FileUtils.getFileContent(fullPath)
        throw new TaskNotImplementedException();
    }
}
