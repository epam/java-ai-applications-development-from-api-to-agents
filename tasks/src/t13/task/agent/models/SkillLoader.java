package t13.task.agent.models;

import org.yaml.snakeyaml.Yaml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SkillLoader {

    private static final Pattern NAME_RE = Pattern.compile("^[a-z0-9][a-z0-9-]*[a-z0-9]$|^[a-z0-9]$");

    public static List<SkillMetadata> loadSkills(Path skillsDir) {
        List<SkillMetadata> skills = new ArrayList<>();

        if (!Files.isDirectory(skillsDir)) {
            System.out.println("WARN: skills directory not found: " + skillsDir.toAbsolutePath());
            return skills;
        }

        try {
            List<Path> skillDirs = Files.list(skillsDir)
                    .filter(Files::isDirectory)
                    .sorted()
                    .collect(Collectors.toList());

            for (Path skillDir : skillDirs) {
                Path skillMd = skillDir.resolve("SKILL.md");
                if (!Files.exists(skillMd)) {
                    System.out.println("WARN: skipping '" + skillDir.getFileName() + "' — no SKILL.md");
                    continue;
                }

                String content = Files.readString(skillMd);
                if (!content.startsWith("---")) {
                    System.out.println("WARN: skipping '" + skillDir.getFileName() + "' — missing YAML frontmatter");
                    continue;
                }

                int end = content.indexOf("---", 3);
                if (end < 0) {
                    System.out.println("WARN: skipping '" + skillDir.getFileName() + "' — unclosed frontmatter");
                    continue;
                }

                String frontmatter = content.substring(3, end);
                Yaml yaml = new Yaml();
                Map<String, Object> fm;
                try {
                    fm = yaml.load(frontmatter);
                } catch (Exception e) {
                    System.out.println("WARN: skipping '" + skillDir.getFileName() + "' — frontmatter parse error: " + e.getMessage());
                    continue;
                }
                if (fm == null) fm = Map.of();

                String name = String.valueOf(fm.getOrDefault("name", ""));
                String description = String.valueOf(fm.getOrDefault("description", "")).strip();
                String compatibility = fm.containsKey("compatibility") ? fm.get("compatibility").toString().strip() : null;
                String dirName = skillDir.getFileName().toString();

                List<String> errors = validate(name, description, compatibility, dirName);
                if (!errors.isEmpty()) {
                    System.out.println("WARN: skipping '" + dirName + "' — " + String.join("; ", errors));
                    continue;
                }

                String license = fm.containsKey("license") ? fm.get("license").toString() : null;

                Map<String, String> metadata = null;
                if (fm.get("metadata") instanceof Map<?, ?> rawMeta) {
                    metadata = rawMeta.entrySet().stream()
                            .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
                }

                List<String> allowedTools = null;
                Object rawAt = fm.get("allowed-tools");
                if (rawAt instanceof String s) {
                    String[] parts = s.trim().split("\\s+");
                    if (parts.length > 0 && !parts[0].isEmpty()) allowedTools = List.of(parts);
                } else if (rawAt instanceof List<?> l) {
                    List<String> list = l.stream().map(Object::toString).collect(Collectors.toList());
                    if (!list.isEmpty()) allowedTools = list;
                }

                skills.add(new SkillMetadata(name, description, skillDir, license, compatibility, metadata, allowedTools));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load skills from " + skillsDir, e);
        }

        return skills;
    }

    private static List<String> validate(String name, String description, String compatibility, String dirName) {
        List<String> errors = new ArrayList<>();

        if (name.isEmpty()) {
            errors.add("name is empty");
        } else if (name.length() > 64) {
            errors.add("name exceeds 64 chars (" + name.length() + ")");
        } else if (!NAME_RE.matcher(name).matches()) {
            errors.add("name contains invalid characters or starts/ends with a hyphen");
        } else if (name.contains("--")) {
            errors.add("name contains consecutive hyphens");
        }

        if (!name.equals(dirName)) {
            errors.add("name '" + name + "' does not match directory name '" + dirName + "'");
        }

        if (description.isEmpty()) {
            errors.add("description is empty");
        } else if (description.length() > 1024) {
            errors.add("description exceeds 1024 chars (" + description.length() + ")");
        }

        if (compatibility != null && compatibility.length() > 500) {
            errors.add("compatibility exceeds 500 chars (" + compatibility.length() + ")");
        }

        return errors;
    }
}
