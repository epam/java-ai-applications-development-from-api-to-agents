package t11.mcp.auth.mcp.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

@SpringBootApplication
public class ApiKeyServerApp {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ApiKeyServerApp.class);
        app.setDefaultProperties(Map.of(
                "server.port", "8007",
                "spring.profiles.active", "api-key-auth",
                "spring.ai.mcp.server.name", "users-management-mcp-server",
                "spring.ai.mcp.server.version", "1.0.0",
                "spring.ai.mcp.server.protocol", "STREAMABLE",
                "logging.level.io.modelcontextprotocol", "DEBUG",
                "logging.level.org.springaicommunity.mcp", "DEBUG",
                "logging.level.t11.mcp.auth", "INFO"
        ));
        app.run(args);
    }
}
