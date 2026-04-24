package com.project.booktour.responses.usersresponse;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AuthResponse {
    private String email;
    private String token;
    private Long roleId;
    private Long userId;
}