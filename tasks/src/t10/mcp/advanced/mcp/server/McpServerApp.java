package t10.mcp.advanced.mcp.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

@SpringBootApplication
public class McpServerApp {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(McpServerApp.class);
        app.setDefaultProperties(Map.of(
                "server.port", "8006",
                "logging.level.t10.mcp.advanced", "DEBUG"
        ));
        app.run(args);
    }
}
