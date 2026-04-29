package t13.task.agent.models;

import java.util.Map;

public record McpToolModel(String name, String description, Map<String, Object> parameters) {}
