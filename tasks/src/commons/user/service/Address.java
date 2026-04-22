package commons.user.service;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Address(
        String country,
        String city,
        String street,
        @JsonProperty("flat_house") String flatHouse
) {}
