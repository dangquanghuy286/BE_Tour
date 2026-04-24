package com.project.booktour.configurations;

import com.project.booktour.files.JwtTokenFilter;
import com.project.booktour.models.CustomOAuth2User;
import com.project.booktour.services.user.IOAuth2UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.springframework.http.HttpMethod.*;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {
    private final JwtTokenFilter jwtTokenFilter;
    private final IOAuth2UserService oAuth2UserService;

    @Value("${api.prefix}")
    private String apiPrefix;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .securityContext(context -> context
                        .requireExplicitSave(false)
                )
                .authorizeHttpRequests(requests -> {
                    requests
                            .requestMatchers(
                                    String.format("%s/users/register", apiPrefix),
                                    String.format("%s/users/login", apiPrefix),
                                    String.format("%s/users/forgot-password", apiPrefix),
                                    String.format("%s/users/reset-password", apiPrefix),
                                    String.format("%s/users/verify-reset-token", apiPrefix),
                                    String.format("%s/users/activate", apiPrefix),
                                    String.format("%s/users/resend-activation", apiPrefix),
                                    String.format("%s/tours/**", apiPrefix),
                                    String.format("%s/banners/**", apiPrefix), // Allow public GET for banners
                                    String.format("%s/banners/images/**", apiPrefix), // Allow public access to banner images
                                    "/favicon.ico",
                                    "/login/oauth2/code/*",
                                    String.format("%s/payment/vnpay-payment-callback", apiPrefix),
                                    String.format("%s/users/avatars/**", apiPrefix)
                            ).permitAll()
                            .requestMatchers(
                                    String.format("%s/chat", apiPrefix),
                                    String.format("%s/events", apiPrefix),
                                    String.format("%s/receive-message", apiPrefix),
                                    String.format("%s/health/n8n", apiPrefix)
                            ).permitAll()
                            .requestMatchers(GET, String.format("%s/tours/**", apiPrefix)).permitAll()
                            .requestMatchers(POST, String.format("%s/tours/**", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(PUT, String.format("%s/tours/**", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(DELETE, String.format("%s/tours/**", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(POST, String.format("%s/banners/**", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(PUT, String.format("%s/banners/**", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(DELETE, String.format("%s/banners/**", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(POST, String.format("%s/booking/**", apiPrefix)).hasRole("USER")
                            .requestMatchers(GET, String.format("%s/booking/**", apiPrefix)).hasAnyRole("USER", "ADMIN")
                            .requestMatchers(PUT, String.format("%s/booking/**", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(DELETE, String.format("%s/booking/**", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(GET, String.format("%s/promotion/**", apiPrefix)).hasAnyRole("USER", "ADMIN")
                            .requestMatchers(POST, String.format("%s/promotion/**", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(PUT, String.format("%s/promotion/**", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(DELETE, String.format("%s/promotion/**", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(GET, String.format("%s/reviews/**", apiPrefix)).permitAll()
                            .requestMatchers(POST, String.format("%s/reviews/**", apiPrefix)).hasRole("USER")
                            .requestMatchers(PUT, String.format("%s/reviews/**", apiPrefix)).hasRole("USER")
                            .requestMatchers(DELETE, String.format("%s/reviews/**", apiPrefix)).hasRole("USER")
                            .requestMatchers(GET, String.format("%s/history/**", apiPrefix)).hasAnyRole("USER", "ADMIN")
                            .requestMatchers(POST, String.format("%s/history/**", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(PUT, String.format("%s/history/**", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(DELETE, String.format("%s/history/**", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(GET, String.format("%s/invoices/**", apiPrefix)).hasAnyRole("USER", "ADMIN")
                            .requestMatchers(POST, String.format("%s/invoices/**", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(PUT, String.format("%s/invoices/**", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(DELETE, String.format("%s/invoices/**", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(POST, String.format("%s/checkout/**", apiPrefix)).hasRole("USER")
                            .requestMatchers(GET, String.format("%s/checkout/**", apiPrefix)).hasAnyRole("USER", "ADMIN")
                            .requestMatchers(PUT, String.format("%s/checkout/**", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(DELETE, String.format("%s/checkout/**", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(GET, String.format("%s/tokens/**", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(POST, String.format("%s/tokens/**", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(PUT, String.format("%s/tokens/**", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(DELETE, String.format("%s/tokens/**", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(GET, String.format("%s/social_accounts/**", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(POST, String.format("%s/social_accounts/**", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(PUT, String.format("%s/social_accounts/**", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(DELETE, String.format("%s/social_accounts/**", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(POST, String.format("%s/users/activate-by-admin", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(GET, String.format("%s/contact/**", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(POST, String.format("%s/contact/**", apiPrefix)).permitAll()
                            .requestMatchers(PUT, String.format("%s/contact/**", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(DELETE, String.format("%s/contact/**", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(GET, String.format("%s/dashboard", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(GET, String.format("%s/dashboard/stats/range", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(GET, String.format("%s/blogs/**", apiPrefix)).permitAll()
                            .requestMatchers(POST, String.format("%s/blogs/**", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(PUT, String.format("%s/blogs/**", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(DELETE, String.format("%s/blogs/**", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(GET, String.format("%s/guides/**", apiPrefix)).permitAll()
                            .requestMatchers(POST, String.format("%s/guides/**", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(PUT, String.format("%s/guides/**", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(DELETE, String.format("%s/guides/**", apiPrefix)).hasRole("ADMIN")
                            .requestMatchers(GET, String.format("%s/guides/images/**", apiPrefix)).permitAll()
                            .anyRequest().authenticated();
                })
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Token không hợp lệ hoặc thiếu\"}");
                        })
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(oAuth2UserService)
                        )
                        .successHandler((request, response, authentication) -> {
                            CustomOAuth2User oauthUser = (CustomOAuth2User) authentication.getPrincipal();
                            String jwtToken = oauthUser.getJwtToken();
                            String userName = oauthUser.getName();
                            String email = oauthUser.getEmail();
                            Long roleId = oauthUser.getRoleId();
                            Long userId = oauthUser.getUserId();

                            String roleIdStr = roleId != null ? roleId.toString() : "default_role_id";
                            String userIdStr = userId != null ? userId.toString() : "default_user_id";

                            String redirectUrl = String.format(
                                    "http://localhost:5173/redirect?token=%s&user_name=%s&email=%s&roleId=%s&userId=%s",
                                    URLEncoder.encode(jwtToken, StandardCharsets.UTF_8),
                                    URLEncoder.encode(userName, StandardCharsets.UTF_8),
                                    URLEncoder.encode(email, StandardCharsets.UTF_8),
                                    URLEncoder.encode(roleIdStr, StandardCharsets.UTF_8),
                                    URLEncoder.encode(userIdStr, StandardCharsets.UTF_8)
                            );
                            response.sendRedirect(redirectUrl);
                        })
                        .failureHandler((request, response, exception) -> {
                            response.sendRedirect("http://localhost:5173/login?error=true");
                        })
                )
                .formLogin(form -> form.disable());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000", "https://attackseawaysss.app.n8n.cloud"));
        configuration.setExposedHeaders(List.of("Content-Type", "Cache-Control", "Connection", "Transfer-Encoding"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}