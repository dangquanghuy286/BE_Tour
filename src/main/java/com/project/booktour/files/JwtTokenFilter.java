package com.project.booktour.files;

import com.project.booktour.components.JwtTokenUtil;
import com.project.booktour.models.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenFilter.class);
    @Value("${api.prefix}")
    private String apiPrefix;
    private final UserDetailsService userDetailsService;
    private final JwtTokenUtil jwtTokenUtil;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        logger.info("Processing request: {} {}", request.getMethod(), request.getServletPath());
        if (isBypassToken(request)) {
            logger.info("Bypassing token check for: {}", request.getServletPath());
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        logger.info("Authorization header: {}", authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ") || authHeader.length() <= 7) {
            logger.warn("Missing or invalid Authorization header");
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Thiếu hoặc không hợp lệ token\"}");
            return;
        }

        final String token = authHeader.substring(7);
        logger.info("JWT Token: {}", token);

        if (token.isEmpty() || !isValidJwtFormat(token)) {
            logger.warn("Invalid JWT token format");
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Token không hợp lệ\"}");
            return;
        }

        try {
            final String userName = jwtTokenUtil.extractUserName(token);
            logger.info("Extracted username: {}", userName);

            if (userName != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                User userDetails = (User) userDetailsService.loadUserByUsername(userName);
                logger.info("Loaded user details: {}, authorities: {}", userDetails.getUsername(), userDetails.getAuthorities());

                if (jwtTokenUtil.validateToken(token, userDetails)) {
                    logger.info("Token validated successfully for user: {}", userName);
                    List<String> authorities = jwtTokenUtil.extractAuthorities(token);
                    logger.info("Token authorities: {}", authorities);
                    List<SimpleGrantedAuthority> grantedAuthorities = authorities.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, grantedAuthorities);
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                } else {
                    logger.warn("Token validation failed for user: {}", userName);
                    response.setContentType("application/json");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Token không hợp lệ\"}");
                    return;
                }
            }
        } catch (Exception e) {
            logger.error("Error processing JWT token: {}", e.getMessage(), e);
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Lỗi xác thực token: " + e.getMessage() + "\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isValidJwtFormat(String token) {
        return token != null && token.split("\\.").length == 3;
    }

    private boolean isBypassToken(@NonNull HttpServletRequest request) {
        final List<Pair<String, String>> bypassTokens = Arrays.asList(
                Pair.of(apiPrefix + "/tours/**", "GET"),
                Pair.of(apiPrefix + "/users/register", "POST"),
                Pair.of(apiPrefix + "/users/login", "POST"),
                Pair.of(apiPrefix + "/users/avatars/**", "GET"),
                Pair.of(apiPrefix + "/users/forgot-password", "POST"),
                Pair.of(apiPrefix + "/users/reset-password", "POST"),
                Pair.of(apiPrefix + "/users/verify-reset-token", "GET"),
                Pair.of(apiPrefix + "/users/activate", "GET"),
                Pair.of(apiPrefix + "/users/resend-activation", "POST"),
                Pair.of(apiPrefix + "/tours/images/**", "GET"),
                Pair.of(apiPrefix + "/reviews/**", "GET"),
                Pair.of(apiPrefix + "/banners/**", "GET"), // Allow public GET for banners
                Pair.of(apiPrefix + "/banners/images/**", "GET"), // Allow public GET for banner images
                Pair.of("/login/oauth2/code/*", "GET"),
                Pair.of(apiPrefix + "/auth/success", "GET"),
                Pair.of(apiPrefix + "/auth/failure", "GET"),
                Pair.of(apiPrefix + "/chat", "POST"),
                Pair.of(apiPrefix + "/events", "GET"),
                Pair.of(apiPrefix + "/receive-message", "POST"),
                Pair.of(apiPrefix + "/health/n8n", "GET"),
                Pair.of(apiPrefix + "/blogs/**", "GET"),
                Pair.of(apiPrefix + "/payment/vnpay-payment-callback", "GET"),
                Pair.of("/login", "GET"),
                Pair.of("/error", "GET"),
                Pair.of("/favicon.ico", "GET"),
                Pair.of(apiPrefix + "/contact/**", "POST"),
                Pair.of(apiPrefix + "/guides/**", "GET"),
                Pair.of(apiPrefix + "/guides/images/**", "GET")
        );
        String requestPath = request.getServletPath();
        String requestMethod = request.getMethod();
        logger.info("Checking bypass for path: {}, method: {}", requestPath, requestMethod);
        for (Pair<String, String> bypassToken : bypassTokens) {
            logger.debug("Comparing with bypass path: {}, method: {}", bypassToken.getFirst(), bypassToken.getSecond());
            if (pathMatcher.match(bypassToken.getFirst(), requestPath) && requestMethod.equals(bypassToken.getSecond())) {
                logger.info("Bypassing token check for: {}", requestPath);
                return true;
            }
        }
        logger.warn("No bypass match for path: {}, method: {}", requestPath, requestMethod);
        return false;
    }
}