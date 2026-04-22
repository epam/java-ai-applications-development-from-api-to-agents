package commons.user.service;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserCreate(
        String name,
        String surname,
        String email,
        String phone,
        @JsonProperty("date_of_birth") String dateOfBirth,
        Address address,
        String gender,
        String company,
        Double salary,
        @JsonProperty("about_me") String aboutMe,
        @JsonProperty("credit_card") CreditCard creditCard
) {}
