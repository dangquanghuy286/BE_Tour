package com.project.booktour.services.user;

import com.project.booktour.components.JwtTokenUtil;
import com.project.booktour.exceptions.InvalidParamException;
import com.project.booktour.models.Role;
import com.project.booktour.models.SocialAccount;
import com.project.booktour.models.User;
import com.project.booktour.models.CustomOAuth2User;
import com.project.booktour.repositories.RoleRepository;
import com.project.booktour.repositories.SocialAccountRepository;
import com.project.booktour.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Service
public class IOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger LOGGER = Logger.getLogger(IOAuth2UserService.class.getName());

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final RoleRepository roleRepository;
    private final JwtTokenUtil jwtTokenUtil;

    public IOAuth2UserService(UserRepository userRepository,
                              SocialAccountRepository socialAccountRepository,
                              RoleRepository roleRepository,
                              JwtTokenUtil jwtTokenUtil) {
        this.userRepository = userRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.roleRepository = roleRepository;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();
        String providerId = oAuth2User.getName();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        LOGGER.info("Processing OAuth2 login: provider=" + provider + ", providerId=" + providerId + ", email=" + email);

        if (email == null || email.isEmpty()) {
            throw new RuntimeException("Email không được cung cấp bởi " + provider);
        }

        // Tìm hoặc tạo người dùng
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            .fullName(name)
                            .userName(email.split("@")[0])
                            .password("") // Mật khẩu rỗng cho OAuth2
                            .isActivated(true) // Đặt true cho người dùng mới
                            .isActive(true)
                            .role(roleRepository.findByName("user")
                                    .orElseGet(() -> {
                                        LOGGER.warning("Role 'user' không tìm thấy, tạo mới role mặc định.");
                                        return roleRepository.save(Role.builder().name("user").build());
                                    }))
                            .build();
                    LOGGER.info("Creating new user: email=" + newUser.getEmail() + ", isActivated=" + newUser.getIsActivated());
                    return newUser;
                });

        // Cập nhật isActivated và loại tài khoản cho người dùng hiện có
        user.setIsActivated(true); // Luôn đặt true cho đăng nhập qua mạng xã hội
        if ("google".equalsIgnoreCase(provider)) {
            user.setGoogleAccountId(1);
            user.setFacebookAccountId(0);
        } else if ("facebook".equalsIgnoreCase(provider)) {
            user.setFacebookAccountId(1);
            user.setGoogleAccountId(0);
        }

        // Lưu người dùng để cập nhật các thay đổi
        User savedUser = userRepository.save(user);
        LOGGER.info("Saved user: email=" + savedUser.getEmail() + ", isActivated=" + savedUser.getIsActivated() +
                ", GoogleAccountId=" + savedUser.getGoogleAccountId() + ", FacebookAccountId=" + savedUser.getFacebookAccountId());

        // Kiểm tra và lưu SocialAccount
        Optional<SocialAccount> existingSocialAccount = socialAccountRepository.findByProviderAndProviderId(provider, providerId);
        SocialAccount socialAccount;
        if (existingSocialAccount.isPresent()) {
            socialAccount = existingSocialAccount.get();
            socialAccount.setEmail(email);
            socialAccount.setName(name);
            socialAccount.setUser(savedUser);
            LOGGER.info("Updating existing SocialAccount: providerId=" + socialAccount.getProviderId());
        } else {
            socialAccount = SocialAccount.builder()
                    .provider(provider)
                    .providerId(providerId)
                    .email(email)
                    .name(name)
                    .user(savedUser)
                    .build();
            LOGGER.info("Creating new SocialAccount: provider=" + provider + ", providerId=" + providerId);
        }
        socialAccountRepository.save(socialAccount);

        // Tạo JWT token
        String jwtToken;
        try {
            jwtToken = jwtTokenUtil.generateToken(savedUser);
            LOGGER.info("Generated JWT token for user: " + savedUser.getUsername());
        } catch (InvalidParamException e) {
            LOGGER.severe("Failed to generate JWT token: " + e.getMessage());
            throw new RuntimeException("Không thể tạo JWT token", e);
        }

        // Tạo CustomOAuth2User
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + savedUser.getRole().getName().toUpperCase())
        );

        return new CustomOAuth2User(
                savedUser.getUsername(),
                authorities,
                jwtToken,
                savedUser.getEmail(),
                savedUser.getRole().getRoleId(),
                savedUser.getUserId()
        );
    }
}