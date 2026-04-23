package t6.grounding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record User(
        int id,
        String name,
        String surname,
        String email,
        String phone,
        @JsonProperty("date_of_birth") String dateOfBirth,
        String address,
        String gender,
        String company,
        String salary,
        @JsonProperty("about_me") String aboutMe,
        @JsonProperty("credit_card") String creditCard
) {
    public String toDocument() {
        return "User:\n" +
               "  id: " + id + "\n" +
               "  name: " + name + "\n" +
               "  surname: " + surname + "\n" +
               "  email: " + email + "\n" +
               "  phone: " + phone + "\n" +
               "  date_of_birth: " + dateOfBirth + "\n" +
               "  address: " + address + "\n" +
               "  gender: " + gender + "\n" +
               "  company: " + company + "\n" +
               "  salary: " + salary + "\n" +
               "  about_me: " + aboutMe + "\n";
    }

    public String toHobbyDocument() {
        return "User:\n id: " + id + ",\nAbout user: " + aboutMe + "\n";
    }
}
