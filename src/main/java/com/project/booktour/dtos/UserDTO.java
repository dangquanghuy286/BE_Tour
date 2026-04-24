package com.project.booktour.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDTO {
    @NotBlank(message = "Username is required")
    @JsonProperty("user_name")
    private String userName;


    @JsonProperty("full_name")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password cannot be blank")
    private String password;

    @JsonProperty("confirm_password")
    private String confirmPassword;

    @JsonProperty("facebook_account_id")
    private Integer facebookAccountId;

    @JsonProperty("google_account_id")
    private Integer googleAccountId;

    @NotNull(message = "Role ID is required")
    @JsonProperty("roleId")
    private Long roleId;

    private String phoneNumber;

    private String address;

    @JsonProperty("date_of_birth")
    private LocalDate dateOfBirth;

    @JsonProperty("is_active")
    private Boolean isActive;


    @JsonProperty("is_activated")
    private Boolean isActivated;
}