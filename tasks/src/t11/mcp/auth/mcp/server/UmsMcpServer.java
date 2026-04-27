package t11.mcp.auth.mcp.server;

import commons.user.service.UserCreate;
import commons.user.service.UserSearchRequest;
import commons.user.service.UserServiceClient;
import commons.user.service.UserUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpPrompt;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

@Component
public class UmsMcpServer {

    private static final Logger log = LoggerFactory.getLogger(UmsMcpServer.class);

    // ==================== TOOL DEFINITIONS ====================

    @McpTool(name = "get_user_by_id", description = "Provides full user information by id")
    public String getUserById(@McpToolParam int userId) {
        log.info("Tool [get_user_by_id] invoked with userId={}", userId);
        try {
            String result = new UserServiceClient().getUser(userId);
            log.info("Tool [get_user_by_id] completed for userId={}", userId);
            return result;
        } catch (Exception e) {
            log.error("Tool [get_user_by_id] failed for userId={}: {}", userId, e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }

    @McpTool(name = "delete_user", description = "Deletes user")
    public String deleteUser(@McpToolParam int userId) {
        log.info("Tool [delete_user] invoked with userId={}", userId);
        try {
            String result = new UserServiceClient().deleteUser(userId);
            log.info("Tool [delete_user] completed for userId={}", userId);
            return result;
        } catch (Exception e) {
            log.error("Tool [delete_user] failed for userId={}: {}", userId, e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }

    @McpTool(name = "search_user", description = "Searches for users by name, surname, email and gender")
    public String searchUser(@McpToolParam UserSearchRequest userSearchRequest) {
        log.info("Tool [search_user] invoked with request={}", userSearchRequest);
        try {
            String result = new UserServiceClient().searchUsers(
                    userSearchRequest.name(),
                    userSearchRequest.surname(),
                    userSearchRequest.email(),
                    userSearchRequest.gender()
            );
            log.info("Tool [search_user] completed");
            return result;
        } catch (Exception e) {
            log.error("Tool [search_user] failed: {}", e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }

    @McpTool(name = "add_user", description = "Adds new user into the system")
    public String addUser(@McpToolParam UserCreate userCreate) {
        log.info("Tool [add_user] invoked for email={}", userCreate.email());
        try {
            String result = new UserServiceClient().addUser(userCreate);
            log.info("Tool [add_user] completed for email={}", userCreate.email());
            return result;
        } catch (Exception e) {
            log.error("Tool [add_user] failed for email={}: {}", userCreate.email(), e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }

    @McpTool(name = "update_user", description = "Updates user by userId")
    public String updateUser(@McpToolParam int userId, @McpToolParam UserUpdate userUpdate) {
        log.info("Tool [update_user] invoked with userId={}", userId);
        try {
            String result = new UserServiceClient().updateUser(userId, userUpdate);
            log.info("Tool [update_user] completed for userId={}", userId);
            return result;
        } catch (Exception e) {
            log.error("Tool [update_user] failed for userId={}: {}", userId, e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }

    // ==================== MCP RESOURCES ====================

    @McpResource(
            uri = "users-management://flow-diagram",
            name = "flow-diagram",
            mimeType = "image/png",
            description = "The Users Management Service flow diagram as PNG image"
    )
    public String flowDiagramResource() {
        try {
            Path imagePath = Path.of("tasks/src/t9/mcp/fundamentals/flow.png");
            byte[] bytes = Files.readAllBytes(imagePath);
            return Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read flow.png: " + e.getMessage(), e);
        }
    }

    // ==================== MCP PROMPTS ====================

    @McpPrompt(description = "users formulate effective search queries")
    public String searchAssistantPrompt() {
        return """
                You are helping users search through a dynamic user database. The database contains
                realistic synthetic user profiles with the following searchable fields:
                
                ## Available Search Parameters
                - **name**: First name (partial matching, case-insensitive)
                - **surname**: Last name (partial matching, case-insensitive)
                - **email**: Email address (partial matching, case-insensitive)
                - **gender**: Exact match (male, female, other, prefer_not_to_say)
                
                ## Search Strategy Guidance
                
                ### For Name Searches
                - Use partial names: "john" finds John, Johnny, Johnson, etc.
                - Try common variations: "mike" vs "michael", "liz" vs "elizabeth"
                
                ### For Email Searches
                - Search by domain: "gmail" for all Gmail users
                - Search by name patterns: "john" for emails containing john
                
                ### Effective Search Combinations
                - Name + Gender: Find specific demographic segments
                - Email domain + Surname: Find business contacts
                
                ## Tips for Better Results
                1. Start broad, then narrow down
                2. Try variations of names (John vs Johnny)
                3. Combine multiple criteria for precision
                4. Remember searches are case-insensitive
                """;
    }

    @McpPrompt(description = "Guides creation of realistic user profiles")
    public String profileCreationPrompt() {
        return """
                You are helping create realistic user profiles for the system. Follow these guidelines
                to ensure data consistency and realism.
                
                ## Required Fields
                - **name**: 2-50 characters, letters only, culturally appropriate
                - **surname**: 2-50 characters, letters only
                - **email**: Valid format, must be unique in system
                - **about_me**: Rich, realistic biography
                
                ## Optional Fields Best Practices
                - **phone**: Use E.164 format (+1234567890)
                - **date_of_birth**: YYYY-MM-DD format, realistic ages (18-80)
                - **gender**: male, female, other, prefer_not_to_say
                - **company**: Real-sounding company names
                - **salary**: $30,000-$200,000 range
                
                ## Address Guidelines
                - **country**, **city**, **street**, **flat_house**: Use realistic values
                
                ## Credit Card Guidelines
                - **num**: XXXX-XXXX-XXXX-XXXX format (non-functional)
                - **cvv**: 3 digits
                - **exp_date**: MM/YYYY format, future dates only
                
                Aim for diversity in geographic, age, and cultural representation.
                """;
    }

}
