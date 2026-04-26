package commons.user.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserUpdate(
        @JsonProperty(value = "name", required = false) String name,
        @JsonProperty(value = "surname", required = false) String surname,
        @JsonProperty(value = "email", required = false) String email,
        @JsonProperty(value = "phone", required = false) String phone,
        @JsonProperty(value = "date_of_birth", required = false) String dateOfBirth,
        @JsonProperty(value = "address", required = false) Address address,
        @JsonProperty(value = "gender", required = false) String gender,
        @JsonProperty(value = "company", required = false) String company,
        @JsonProperty(value = "salary", required = false) Double salary,
        @JsonProperty(value = "credit_card", required = false) CreditCard creditCard
) {}
