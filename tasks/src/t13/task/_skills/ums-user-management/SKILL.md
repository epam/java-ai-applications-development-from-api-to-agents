---
name: ums-user-management
description: >
  TODO 1 — Write a one-to-three sentence description: what the skill does (manages users in the UMS),
  which operations it supports (CRUD, search, web enrichment), and when it should be activated
  (user asks to find, add, update, or remove users)
license: Apache-2.0
metadata:
  author: ai-powered-apps-development-expert
  version: "2.0"
---

# UMS User Management

<!-- TODO 2 — Write an introductory paragraph naming the agent's role and listing BOTH MCP servers it has access to
     (UMS MCP Server for CRUD, DuckDuckGo Search MCP Server for web enrichment) -->

---

## MCP Server Connections

<!-- TODO 3 — Add a Markdown table with three columns: Server | Transport | URL
     Row 1: UMS MCP Server           | streamable-http | http://localhost:8005/mcp
     Row 2: DuckDuckGo Search MCP Server | streamable-http | http://localhost:8000/mcp -->

---

## Available MCP Tools

### UMS MCP Server Tools

<!-- TODO 4 — Add a table listing all five UMS tools with columns: Tool | Description | Key Parameters
     Tools to document:
       get_user_by_id  — Fetch full user profile by ID          — user_id: int
       search_user     — Search by name/surname/email/gender    — search_user_request: UserSearchRequest
       add_user        — Create a new user record               — user_create_model: UserCreate
       update_user     — Update fields on an existing user      — user_id: int, user_update_model: UserUpdate
       delete_user     — Permanently delete a user by ID        — user_id: int

     Below the table add three bullet blocks:
     - UserCreate REQUIRED fields: name, surname, email, about_me
     - UserCreate OPTIONAL fields: phone, date_of_birth, address (country/city/street/flat_house),
       gender, company, salary, credit_card (num, cvv, exp_date)
     - UserSearchRequest fields (all optional, partial case-insensitive except gender exact):
       name, surname, email, gender (exact: male/female/other/prefer_not_to_say)
     - UserUpdate: same optional fields as UserCreate (pass only fields to change) -->

---

### DuckDuckGo Search MCP Server Tools

<!-- TODO 5 — Add a table listing both DuckDuckGo tools with columns: Tool | Description | Key Parameters
     Tools to document:
       search        — Search DuckDuckGo, returns titles/URLs/snippets — query: str, max_results: int (default 10, max 50)
       fetch_content — Fetch clean text from a webpage                 — url: str (must start with http:// or https://)
     After the table, add one line each explaining:
       - Use search to find missing user information (bio, company, contact details)
       - Use fetch_content to retrieve deeper details from a specific URL returned by search -->

---

## Operating Rules

<!-- TODO 6 — Number the operating rules 1–7:
     1. Always explain your actions before executing any tool call
     2. UMS first: always query UMS before resorting to web search
     3. Web search for enrichment: when adding a user and information is incomplete/ambiguous,
        use DuckDuckGo search (and optionally fetch_content) to fill in missing details
     4. Confirm before creating: present the full proposed user profile to the operator
        and wait for explicit confirmation before calling add_user
     5. Deletions require confirmation: always warn the operator that deletion is permanent
        and irreversible before calling delete_user
     6. Format responses clearly: present user data in a structured, readable format
     7. Handle errors gracefully: explain what went wrong and suggest alternatives -->

---

## Workflows

### Finding a User

<!-- TODO 7 — Write the three-step workflow as a numbered code block:
     1. Call search_user with available criteria (name / surname / email / gender)
     2. If results found → present them to the operator
     3. If no results → inform the operator; offer to search the web if context suggests a real person -->

### Adding a User

<!-- TODO 8 — Write the four-step workflow as a numbered code block:
     1. Collect available user data from the operator
     2. Identify missing required fields (name, surname, email, about_me)
     3. If data is incomplete:
        a. Call search (DuckDuckGo) with the person's name / company / other context
        b. Optionally call fetch_content on a relevant result URL for deeper details
        c. Build a complete UserCreate profile from gathered data
        d. Present the full profile to the operator for confirmation
     4. On confirmation → call add_user -->

### Updating a User

<!-- TODO 9 — Write the four-step workflow as a numbered code block:
     1. If user_id is unknown → call search_user to locate the user first
     2. Confirm which fields to update with the operator
     3. Call update_user with only the fields that need to change
     4. Report success or explain any error -->

### Deleting a User

<!-- TODO 10 — Write the five-step workflow as a numbered code block:
     1. If user_id is unknown → call search_user to locate the user first
     2. Display the user's details and warn: "This action is permanent and cannot be undone."
     3. Wait for explicit operator confirmation
     4. On confirmation → call delete_user
     5. Report success or explain any error -->

---

## Boundaries

<!-- TODO 11 — Write one or two sentences defining the agent's scope:
     the agent specializes in user management only; for unrelated requests,
     politely redirect the operator to the core capabilities -->
