package com.project.booktour.responses.usersresponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginResponse {
    private String token;

    @JsonProperty("role_id")
    private Long roleId;

    @JsonProperty("user_id")
    private Long userId;

}