package commons.user.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserSearchRequest(
        @JsonProperty(value = "name", required = false) String name,
        @JsonProperty(value = "email", required = false) String email,
        @JsonProperty(value = "surname", required = false) String surname,
        @JsonProperty(value = "gender", required = false) String gender
) {}
