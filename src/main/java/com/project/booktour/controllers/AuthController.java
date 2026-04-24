package com.project.booktour.controllers;

import com.project.booktour.models.CustomOAuth2User;
import com.project.booktour.responses.usersresponse.AuthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix}/auth")
public class AuthController {

    @GetMapping("/success")
    public ResponseEntity<?> loginSuccess(@AuthenticationPrincipal CustomOAuth2User oAuth2User) {
        return ResponseEntity.ok(new AuthResponse(oAuth2User.getEmail(), oAuth2User.getJwtToken(), oAuth2User.getRoleId(), oAuth2User.getUserId()));
    }

    @GetMapping("/failure")
    public ResponseEntity<?> loginFailure() {
        return ResponseEntity.status(401).body("OAuth2 login failed");
    }
}

