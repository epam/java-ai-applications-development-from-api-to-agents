package t10.mcp.advanced.mcp.server.server;

public class McpSession {

    private final String sessionId;
    private boolean readyForOperation;
    private final long createdAt;
    private long lastActivity;

    public McpSession(String sessionId) {
        this.sessionId = sessionId;
        this.readyForOperation = false;
        this.createdAt = System.currentTimeMillis();
        this.lastActivity = createdAt;
    }

    public String getSessionId() { return sessionId; }

    public boolean isReadyForOperation() { return readyForOperation; }
    public void setReadyForOperation(boolean readyForOperation) { this.readyForOperation = readyForOperation; }

    public long getCreatedAt() { return createdAt; }

    public long getLastActivity() { return lastActivity; }
    public void updateActivity() { this.lastActivity = System.currentTimeMillis(); }
}
