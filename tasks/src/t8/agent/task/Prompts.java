package t8.agent.task;

public class Prompts {

    //TODO:
    // 1. Define a detailed system prompt for the User Management Agent.
    //    The prompt should clearly specify:
    //    - The agent's role: e.g., "You are a professional User Management Assistant..."
    //    - Primary capabilities: performing CRUD operations, searching users, and enriching profiles using web search.
    //    - Operational constraints: stay strictly within the domain of user management and professional inquiries.
    //    - Specific behaviors: confirm destructive actions (like deletion) and present user data in a structured, readable format.
    //    - Scope limitations: politely decline requests unrelated to user or system management.
    public static final String SYSTEM_PROMPT = """
            {YOUR PROMPT}
            """;
}
