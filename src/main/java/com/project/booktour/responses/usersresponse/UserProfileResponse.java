package com.project.booktour.responses.usersresponse;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserProfileResponse {

    @JsonProperty("user_name")
    private String userName;
    @JsonProperty("full_name")
    private String fullName;


    @JsonProperty("phone_number")
    private String phoneNumber;

    @Email(message = "Invalid email format")
    private String email;

    @JsonProperty("avatar_path")
    private String avatarPath;

    private String address;

    @JsonProperty("date_of_birth")
    private LocalDate dateOfBirth;

    @JsonProperty("facebook_account_id")
    private Integer facebookAccountId;

    @JsonProperty("google_account_id")
    private Integer googleAccountId;
}