package com.project.booktour.responses.usersresponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.booktour.models.Role;
import jakarta.validation.constraints.Email;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("username") // Changed from full_name to username
    private String username;

    @JsonProperty("full_name") // Added full_name for User.fullName
    private String fullName;

    @JsonProperty("phone_number")
    private String phoneNumber;

    @Email(message = "Invalid email format")
    private String email;

    @JsonProperty("address")
    private String address;

    @JsonProperty("is_active")
    private boolean active;

    @JsonProperty("is_activated")
    private boolean activated;

    @JsonProperty("date_of_birth")
    private LocalDate dateOfBirth;

    @JsonProperty("facebook_account_id")
    private int facebookAccountId;

    @JsonProperty("google_account_id")
    private int googleAccountId;

    @JsonProperty("role")
    private Role role;

    @JsonProperty("avatar")
    private String avatar;

    private static final String BASE_URL = "http://localhost:8088";
    private static final String DEFAULT_AVATAR_URL = "https://images.icon-icons.com/1378/PNG/512/avatardefault_92824.png";

    public static UserResponse fromUser(com.project.booktour.models.User user) {
        String avatarFileName = user.getAvatar();
        String avatarUrl;

        if (avatarFileName != null && !avatarFileName.isEmpty()) {
            String cleanAvatarFileName = avatarFileName
                    .replaceAll("^(?:.*?/)?(?:[Uu]ploads/[Aa]vatars/)?", "");
            avatarUrl = BASE_URL + "/api/v1/users/avatars/" + cleanAvatarFileName;
        } else {
            avatarUrl = DEFAULT_AVATAR_URL;
        }

        return UserResponse.builder()
                .id(user.getUserId())
                .username(user.getUsername()) // Map to username
                .fullName(user.getFullName()) // Map to fullName
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .active(user.getIsActive() != null && user.getIsActive())
                .activated(user.getIsActivated() != null && user.getIsActivated())
                .dateOfBirth(user.getDateOfBirth())
                .facebookAccountId(user.getFacebookAccountId())
                .googleAccountId(user.getGoogleAccountId())
                .role(user.getRole())
                .avatar(avatarUrl)
                .build();
    }
}