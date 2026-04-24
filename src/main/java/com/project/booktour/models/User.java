package com.project.booktour.models;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User extends BaseEntity implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "full_name", length = 50, nullable = false)
    private String fullName;

    @Column(name = "user_name", length = 50, nullable = false)
    private String userName;

    @Column(name = "password", length = 255, nullable = false)
    private String password;

    @Column(name = "email", length = 255, nullable = false)
    private String email;

    @Column(name = "phone_number", length = 15, nullable = false)
    private String phoneNumber;

    @Column(name = "address", length = 255, nullable = false)
    private String address;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_activated")
    private Boolean isActivated = false;

    @Column(name = "activation_token")
    private String activationToken;

    @Column(name = "activation_token_expiration")
    private LocalDateTime activationTokenExpiration;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "facebook_account_id")
    private Integer facebookAccountId = 0;

    @Column(name = "google_account_id")
    private Integer googleAccountId = 0;

    @Column(name = "avatar", length = 255)
    private String avatar;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + getRole().getName().toUpperCase()));
        return authorities;
    }

    @Override
    public String getUsername() {
        return userName;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive && isActivated; // Loại bỏ kiểm tra null vì isActive và isActivated là boolean
    }
}