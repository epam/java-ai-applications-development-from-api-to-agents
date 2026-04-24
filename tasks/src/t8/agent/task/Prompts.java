package t8.agent.task;

public class Prompts {

    //TODO:
    // Provide a system prompt for the User Management Agent.
    // You can use an LLM to help draft it, but review and tailor the generated prompt carefully.
    // ---
    // A good system prompt should define:
    //   - The agent's role (e.g., "You are a User Management Agent...")
    //   - Primary tasks: CRUD operations, search, profile enrichment via web search
    //   - Constraints: stay within user management domain, avoid sensitive personal data
    //   - Behavioral patterns:
    //       * confirm before destructive actions (e.g., delete)
    //       * structured, readable output when displaying user info
    //       * professional tone with clear error messages and next-step suggestions
    //       * ask for clarification when search criteria are ambiguous
    //   - Scope limitations: decline unrelated requests, redirect to user management
    // Keep it concise and domain-focused.
    public static final String SYSTEM_PROMPT = """
            {YOUR PROMPT}
            """;
}
