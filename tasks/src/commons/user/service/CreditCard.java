package commons.user.service;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreditCard(
        String num,
        String cvv,
        @JsonProperty("exp_date") String expDate
) {}
