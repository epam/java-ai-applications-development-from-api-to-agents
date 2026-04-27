package t10.mcp.advanced.mcp.server.server.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpResponse {

    @JsonProperty("jsonrpc")
    private String jsonrpc = "2.0";

    @JsonProperty("id")
    private Object id;

    @JsonProperty("result")
    private Map<String, Object> result;

    @JsonProperty("error")
    private McpError error;

    public McpResponse() {}

    public McpResponse(Object id, Map<String, Object> result) {
        this.id = id;
        this.result = result;
    }

    public McpResponse(Object id, McpError error) {
        this.id = id;
        this.error = error;
    }

    public String getJsonrpc() { return jsonrpc; }
    public void setJsonrpc(String jsonrpc) { this.jsonrpc = jsonrpc; }

    public Object getId() { return id; }
    public void setId(Object id) { this.id = id; }

    public Map<String, Object> getResult() { return result; }
    public void setResult(Map<String, Object> result) { this.result = result; }

    public McpError getError() { return error; }
    public void setError(McpError error) { this.error = error; }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class McpError {

        @JsonProperty("code")
        private int code;

        @JsonProperty("message")
        private String message;

        @JsonProperty("data")
        private Object data;

        public McpError(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public int getCode() { return code; }
        public void setCode(int code) { this.code = code; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
    }
}
