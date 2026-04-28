package t12.skills.task.custom;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class SkillLoader {

    private static final Pattern NAME_RE = Pattern.compile("^[a-z0-9][a-z0-9-]*[a-z0-9]$|^[a-z0-9]$");

    public static List<SkillMetadata> loadSkills(Path skillsDir) {
        List<SkillMetadata> skills = new ArrayList<>();

        try (var stream = Files.list(skillsDir)) {
            stream.filter(Files::isDirectory).sorted().forEach(skillDir -> {
                Path skillMd = skillDir.resolve("SKILL.md");
                if (!Files.exists(skillMd)) {
                    System.out.println("WARN: skipping '" + skillDir.getFileName() + "' — no SKILL.md");
                    return;
                }

                String content;
                try {
                    content = Files.readString(skillMd);
                } catch (IOException e) {
                    System.out.println("WARN: skipping '" + skillDir.getFileName() + "' — read error: " + e.getMessage());
                    return;
                }

                if (!content.startsWith("---")) {
                    System.out.println("WARN: skipping '" + skillDir.getFileName() + "' — missing YAML frontmatter");
                    return;
                }

                int end = content.indexOf("---", 3);
                if (end < 0) {
                    System.out.println("WARN: skipping '" + skillDir.getFileName() + "' — unclosed frontmatter");
                    return;
                }

                Map<String, Object> fm;
                try {
                    fm = new Yaml().load(content.substring(3, end));
                } catch (Exception e) {
                    System.out.println("WARN: skipping '" + skillDir.getFileName() + "' — frontmatter parse error: " + e.getMessage());
                    return;
                }
                if (fm == null) fm = Map.of();

                String name = fm.containsKey("name") ? String.valueOf(fm.get("name")).strip() : "";
                String description = fm.containsKey("description") ? String.valueOf(fm.get("description")).strip() : "";
                String compatibility = fm.containsKey("compatibility") ? String.valueOf(fm.get("compatibility")).strip() : null;
                String dirName = skillDir.getFileName().toString();

                List<String> errors = validate(name, description, compatibility, dirName);
                if (!errors.isEmpty()) {
                    System.out.println("WARN: skipping '" + dirName + "' — " + String.join("; ", errors));
                    return;
                }

                List<String> allowedTools = null;
                if (fm.containsKey("allowed-tools")) {
                    Object raw = fm.get("allowed-tools");
                    if (raw instanceof String s) {
                        List<String> parts = List.of(s.trim().split("\\s+"));
                        allowedTools = parts.isEmpty() ? null : new ArrayList<>(parts);
                    } else if (raw instanceof List<?> list) {
                        List<String> parts = list.stream().map(Object::toString).toList();
                        allowedTools = parts.isEmpty() ? null : parts;
                    }
                }

                @SuppressWarnings("unchecked")
                Map<String, String> metadata = fm.containsKey("metadata")
                        ? (Map<String, String>) fm.get("metadata")
                        : null;

                skills.add(new SkillMetadata(
                        name, description, skillDir,
                        fm.containsKey("license") ? String.valueOf(fm.get("license")) : null,
                        compatibility, metadata, allowedTools
                ));
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to list skills directory: " + skillsDir, e);
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
