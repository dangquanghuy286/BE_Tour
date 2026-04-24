package com.project.booktour.models;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

public class CustomOAuth2User implements OAuth2User {
    private final String name;
    private final Collection<? extends GrantedAuthority> authorities;
    private final String jwtToken;
    private final String email;
    private final Long roleId;
    private final Long userId; // Thêm userId

    public CustomOAuth2User(String name, Collection<? extends GrantedAuthority> authorities, String jwtToken, String email, Long roleId, Long userId) {
        this.name = name;
        this.authorities = authorities;
        this.jwtToken = jwtToken;
        this.email = email;
        this.roleId = roleId;
        this.userId = userId; // Khởi tạo userId
    }

    @Override
    public Map<String, Object> getAttributes() {
        return null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public String getEmail() {
        return email;
    }

    public Long getRoleId() {
        return roleId;
    }

    public Long getUserId() { // Getter cho userId
        return userId;
    }
}