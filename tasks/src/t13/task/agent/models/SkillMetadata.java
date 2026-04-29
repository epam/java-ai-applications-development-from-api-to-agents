package t13.task.agent.models;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record SkillMetadata(
        String name,
        String description,
        Path skillDir,
        String license,
        String compatibility,
        Map<String, String> metadata,
        List<String> allowedTools
) {}
