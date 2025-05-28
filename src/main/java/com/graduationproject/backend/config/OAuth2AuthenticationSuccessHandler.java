// src/main/java/com/graduationproject/backend/config/OAuth2AuthenticationSuccessHandler.java
package com.graduationproject.backend.config;

import com.graduationproject.backend.service.UserService;
import com.graduationproject.backend.util.CustomUserDetails;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    @Autowired private JwtTokenProvider tokenProvider;
    @Autowired private UserService userService;

    @Value("${app.oauth2.frontendRedirectUri}")
    private String frontendRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (!(authentication instanceof OAuth2AuthenticationToken)) {
            logger.error("OAuth2 Success Handler called with invalid token type: {}", authentication.getClass().getName());

            String errorUrl = UriComponentsBuilder
                    .fromUriString(frontendRedirectUri + "/oauth2/redirect")
                    .queryParam("error", "LoginFailed")
                    .build().toUriString();

            getRedirectStrategy().sendRedirect(request, response, errorUrl);
            return;
        }

        Object principal = authentication.getPrincipal();
        String username = null;

        if (principal instanceof CustomUserDetails) {
            username = ((CustomUserDetails) principal).getUsername();
        } else if (principal instanceof OAuth2User) {
            username = ((OAuth2User) principal).getAttribute("email"); // Fallback
            logger.warn("Principal is default OAuth2User, using email: {}", username);
        }

        if (username == null || username.isBlank()) {
            logger.error("Cannot extract username from principal: {}", principal);
            String errorUrl = UriComponentsBuilder.fromUriString(frontendRedirectUri).queryParam("error", "LoginFailed").build().toUriString();
            getRedirectStrategy().sendRedirect(request, response, errorUrl);
            return;
        }

        // Tạo JWT token
        String jwt = tokenProvider.generateToken(username);
        logger.info("Generated JWT for OAuth2 user: {}", username);

        // Tạo URL redirect về Frontend kèm token
        String targetUrl = UriComponentsBuilder.fromUriString(frontendRedirectUri + "/oauth2/redirect")
                .queryParam("token", jwt)
                .build().toUriString();
        logger.info("Token: ", jwt);

        clearAuthenticationAttributes(request);
        logger.info("Redirecting OAuth2 user to: {}", targetUrl);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}