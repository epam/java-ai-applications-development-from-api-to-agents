package commons.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record Message(
        Role role,
        String content,
        String toolCallId,
        String name,
        List<Map<String, Object>> toolCalls
) {

    public Message(Role role, String content) {
        this(role, content, null, null, null);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("role", role.getValue());
        result.put("content", content);
        if (toolCallId != null) {
            result.put("tool_call_id", toolCallId);
        }
        if (name != null) {
            result.put("name", name);
        }
        if (toolCalls != null) {
            result.put("tool_calls", toolCalls);
        }
        return result;
    }
}
