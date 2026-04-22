package commons.user.service;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserUpdate(
        String name,
        String surname,
        String email,
        String phone,
        @JsonProperty("date_of_birth") String dateOfBirth,
        Address address,
        String gender,
        String company,
        Double salary,
        @JsonProperty("credit_card") CreditCard creditCard
) {}
