package commons.user.service;

public record UserSearchRequest(
        String name,
        String email,
        String surname,
        String gender
) {}
