package t13.task.agent;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Message {
    private String role;
    private String content;
    @JsonProperty("tool_call_id")
    private String toolCallId;
    @JsonProperty("tool_calls")
    private List<Map<String, Object>> toolCalls;

    public Message() {}

    public Message(String role, String content, String toolCallId, List<Map<String, Object>> toolCalls) {
        this.role = role;
        this.content = content;
        this.toolCallId = toolCallId;
        this.toolCalls = toolCalls;
    }

    public Map<String, Object> toDict() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("role", role);
        if (content != null) map.put("content", content);
        if (toolCallId != null) map.put("tool_call_id", toolCallId);
        if (toolCalls != null) map.put("tool_calls", toolCalls);
        return map;
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getToolCallId() { return toolCallId; }
    public void setToolCallId(String toolCallId) { this.toolCallId = toolCallId; }
    public List<Map<String, Object>> getToolCalls() { return toolCalls; }
    public void setToolCalls(List<Map<String, Object>> toolCalls) { this.toolCalls = toolCalls; }
}
