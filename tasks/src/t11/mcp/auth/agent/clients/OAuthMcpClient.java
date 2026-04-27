package t11.mcp.auth.agent.clients;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * MCP client that authenticates via OAuth 2.0 Authorization Code + PKCE flow (Keycloak).
 *
 * <p>On {@link #connect()}:
 * <ol>
 *   <li>Runs the PKCE browser flow (opens Keycloak login once)
 *   <li>Connects to the MCP server with the resulting Bearer token
 * </ol>
 *
 * <p>On tool calls: proactively checks token expiry and transparently refreshes + reconnects
 * before each call to avoid broken streams mid-session.
 */
public class OAuthMcpClient extends BaseMcpClient {

    // ==================== KEYCLOAK CONFIGURATION ====================

    private static final String KEYCLOAK_URL = envOr("KEYCLOAK_URL", "http://localhost:8089");
    private static final String KEYCLOAK_REALM = envOr("KEYCLOAK_REALM", "mcp-realm");
    private static final String CLIENT_ID = envOr("MCP_CLIENT_ID", "mcp-client");

    private static final int REDIRECT_PORT = 9999;
    private static final String REDIRECT_URI = "http://localhost:" + REDIRECT_PORT + "/callback";
    private static final String BASE = KEYCLOAK_URL + "/realms/" + KEYCLOAK_REALM + "/protocol/openid-connect";
    private static final String AUTH_ENDPOINT = BASE + "/auth";
    private static final String TOKEN_ENDPOINT = BASE + "/token";

    // ==================== TOKEN STATE ====================

    private String accessToken;
    private String storedRefreshToken;
    private long expiresAt;

    private final HttpClient tokenHttpClient = HttpClient.newHttpClient();

    public OAuthMcpClient(String mcpServerUrl) {
        super(mcpServerUrl);
    }

    // ==================== LIFECYCLE ====================

    @Override
    protected void beforeConnect() {
        runPkceFlow();
    }

    @Override
    protected Consumer<HttpRequest.Builder> requestCustomizer() {
        // Lambda captures `this` — reads the current accessToken field on every request,
        // so reconnecting with a refreshed token is transparent to the transport layer.
        return rb -> rb.header("Authorization", "Bearer " + accessToken);
    }

    @Override
    public String callTool(String toolName, Map<String, Object> arguments) {
        if (isTokenExpired()) {
            System.out.println("    Token expired — refreshing and reconnecting...");
            refreshAndReconnect();
        }
        return super.callTool(toolName, arguments);
    }

    // ==================== PKCE FLOW ====================

    private void runPkceFlow() {
        try {
            // Step 1 — Generate PKCE code_verifier + code_challenge (S256)
            byte[] verifierBytes = new byte[64];
            new SecureRandom().nextBytes(verifierBytes);
            String codeVerifier = Base64.getUrlEncoder().withoutPadding().encodeToString(verifierBytes);
            String codeChallenge = sha256Base64Url(codeVerifier);

            // Step 2 — Generate state (CSRF protection)
            byte[] stateBytes = new byte[16];
            new SecureRandom().nextBytes(stateBytes);
            String state = Base64.getUrlEncoder().withoutPadding().encodeToString(stateBytes);

            // Step 3 — Start local callback server in a virtual thread
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<String> codeRef = new AtomicReference<>();
            AtomicReference<String> stateRef = new AtomicReference<>();
            Thread.ofVirtual().start(() -> runCallbackServer(latch, codeRef, stateRef));

            // Step 4 — Open browser to Keycloak authorization endpoint
            String authUrl = buildAuthUrl(codeChallenge, state);
            System.out.println("\nOpening browser for Keycloak login...");
            System.out.println("   URL: " + authUrl + "\n");
            try {
                Desktop.getDesktop().browse(URI.create(authUrl));
            } catch (Exception e) {
                System.out.println("Could not open browser automatically. Please visit the URL above.");
            }

            // Step 5 — Wait for the authorization code (up to 120 s)
            System.out.println("Waiting for authentication callback on http://localhost:9999/callback ...");
            if (!latch.await(120, TimeUnit.SECONDS)) {
                throw new RuntimeException("OAuth callback not received within 120 seconds");
            }

            // Step 6 — Validate state to prevent CSRF
            if (!state.equals(stateRef.get())) {
                throw new RuntimeException("OAuth state mismatch — possible CSRF attack");
            }

            String code = codeRef.get();
            if (code == null) {
                throw new RuntimeException("No authorization code received in callback");
            }

            // Step 7 — Exchange authorization code for tokens
            System.out.println("Exchanging authorization code for tokens...");
            exchangeCodeForTokens(code, codeVerifier);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("OAuth flow interrupted", e);
        }
    }

    // ==================== LOCAL CALLBACK SERVER ====================

    private void runCallbackServer(CountDownLatch latch,
                                   AtomicReference<String> codeRef,
                                   AtomicReference<String> stateRef) {
        try (ServerSocket serverSocket = new ServerSocket(REDIRECT_PORT)) {
            serverSocket.setSoTimeout(120_000);
            try (Socket socket = serverSocket.accept()) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                String requestLine = reader.readLine();

                if (requestLine != null && requestLine.contains("/callback")) {
                    String path = requestLine.split(" ")[1];
                    int queryStart = path.indexOf('?');
                    if (queryStart >= 0) {
                        Map<String, String> params = parseQueryParams(path.substring(queryStart + 1));
                        codeRef.set(params.get("code"));
                        stateRef.set(params.get("state"));
                    }
                    sendBrowserResponse(socket.getOutputStream(), codeRef.get() != null);
                }
            }
            latch.countDown();
        } catch (Exception e) {
            latch.countDown();
        }
    }

    private void sendBrowserResponse(OutputStream out, boolean success) throws IOException {
        String body = success
                ? "<html><body style='font-family:monospace;background:#0a0c10;color:#34d399;"
                        + "display:flex;align-items:center;justify-content:center;"
                        + "height:100vh;margin:0;font-size:18px;'>"
                        + "<div>&#10003; Authentication successful. You can close this tab.</div>"
                        + "</body></html>"
                : "<html><body style='font-family:monospace;background:#0a0c10;color:#f87171;"
                        + "display:flex;align-items:center;justify-content:center;"
                        + "height:100vh;margin:0;font-size:18px;'>"
                        + "<div>&#10007; Authentication failed. Check terminal for details.</div>"
                        + "</body></html>";
        String response = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n"
                + "Content-Length: " + body.length() + "\r\n\r\n" + body;
        out.write(response.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    // ==================== TOKEN EXCHANGE ====================

    @SuppressWarnings("unchecked")
    private void exchangeCodeForTokens(String code, String codeVerifier) {
        try {
            String body = "grant_type=authorization_code"
                    + "&client_id=" + encode(CLIENT_ID)
                    + "&redirect_uri=" + encode(REDIRECT_URI)
                    + "&code=" + encode(code)
                    + "&code_verifier=" + encode(codeVerifier);

            HttpResponse<String> response = tokenHttpClient.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create(TOKEN_ENDPOINT))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(body))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Token exchange failed (" + response.statusCode() + "): " + response.body());
            }

            Map<String, Object> tokens = objectMapper.readValue(response.body(), Map.class);
            storeTokens(tokens);
            System.out.println("Authenticated! Token expires in " + tokens.get("expires_in") + "s\n");

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new RuntimeException("Token exchange failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private void performTokenRefresh() {
        try {
            if (storedRefreshToken == null) {
                throw new RuntimeException("No refresh token available — re-authentication required");
            }
            System.out.println("    Refreshing access token...");

            String body = "grant_type=refresh_token"
                    + "&client_id=" + encode(CLIENT_ID)
                    + "&refresh_token=" + encode(storedRefreshToken);

            HttpResponse<String> response = tokenHttpClient.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create(TOKEN_ENDPOINT))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(body))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

            Map<String, Object> tokens = objectMapper.readValue(response.body(), Map.class);
            storeTokens(tokens);
            System.out.println("    Token refreshed successfully");

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new RuntimeException("Token refresh failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private void storeTokens(Map<String, Object> tokens) {
        this.accessToken = (String) tokens.get("access_token");
        String newRefresh = (String) tokens.get("refresh_token");
        if (newRefresh != null) this.storedRefreshToken = newRefresh;
        int expiresIn = tokens.get("expires_in") instanceof Number n ? n.intValue() : 300;
        this.expiresAt = System.currentTimeMillis() + (expiresIn - 30) * 1000L; // 30 s buffer
    }

    // ==================== TOKEN REFRESH + RECONNECT ====================

    private boolean isTokenExpired() {
        return expiresAt == 0 || System.currentTimeMillis() >= expiresAt;
    }

    private void refreshAndReconnect() {
        performTokenRefresh();
        if (mcpClient != null) {
            try { mcpClient.closeGracefully(); } catch (Exception ignored) {}
        }
        // connectTransport() calls requestCustomizer() which reads the updated accessToken
        connectTransport();
        System.out.println("    Reconnected with fresh token");
    }

    // ==================== HELPERS ====================

    private String buildAuthUrl(String codeChallenge, String state) {
        return AUTH_ENDPOINT
                + "?response_type=code"
                + "&client_id=" + encode(CLIENT_ID)
                + "&redirect_uri=" + encode(REDIRECT_URI)
                + "&scope=" + encode("openid profile")
                + "&state=" + encode(state)
                + "&code_challenge=" + encode(codeChallenge)
                + "&code_challenge_method=S256";
    }

    private static String sha256Base64Url(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                params.put(pair.substring(0, eq), pair.substring(eq + 1));
            }
        }
        return params;
    }

    private static String envOr(String name, String defaultValue) {
        String val = System.getenv(name);
        return (val != null && !val.isBlank()) ? val : defaultValue;
    }
}
