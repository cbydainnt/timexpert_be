// src/main/java/com/graduationproject/backend/config/OAuth2AuthenticationSuccessHandler.java
package com.graduationproject.backend.config;

import com.graduationproject.backend.entity.User;
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

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal(); // Principal luôn là OAuth2User
        String usernameToUseInToken;

        // Kiểm tra xem principal có phải là CustomUserDetails không, nếu vậy thì User entity đã được xử lý
        if (oauth2User instanceof CustomUserDetails) {
            CustomUserDetails customUserDetails = (CustomUserDetails) oauth2User;
            usernameToUseInToken = customUserDetails.getUsername(); // Lấy username từ User entity bên trong
            logger.info("OAuth2 login success. Principal is CustomUserDetails. Username from DB: {}", usernameToUseInToken);
        } else {
            String registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
            logger.warn("OAuth2 login success. Principal is type: {}. Manually processing user with UserService.", oauth2User.getClass().getName());

            User userEntity = userService.processOAuth2User(registrationId, oauth2User); // Gọi lại processOAuth2User
            if (userEntity == null) {
                logger.error("Failed to process OAuth2 user via UserService in SuccessHandler for email: {}", String.valueOf(oauth2User.getAttribute("email")));
                String errorUrl = UriComponentsBuilder.fromUriString(frontendRedirectUri + "/oauth2/redirect").queryParam("error", "UserProcessingError").build().toUriString();
                getRedirectStrategy().sendRedirect(request, response, errorUrl);
                return;
            }
            usernameToUseInToken = userEntity.getUsername();
            logger.info("Manually processed user. Username from DB for token: {}", usernameToUseInToken);
        }

        if (usernameToUseInToken == null || usernameToUseInToken.isBlank()) {
            logger.error("Cannot extract a valid username for token generation from principal: {}", oauth2User.getName());
            String errorUrl = UriComponentsBuilder.fromUriString(frontendRedirectUri + "/oauth2/redirect").queryParam("error", "LoginFailed").build().toUriString();
            getRedirectStrategy().sendRedirect(request, response, errorUrl);
            return;
        }

        String jwt = tokenProvider.generateToken(usernameToUseInToken);
        logger.info("Generated JWT for OAuth2 user, token subject: {}", usernameToUseInToken);

        String targetUrl = UriComponentsBuilder.fromUriString(frontendRedirectUri + "/oauth2/redirect")
                .queryParam("token", jwt)
                .build().toUriString();

        // Sửa dòng log token cho đúng
        logger.info("Generated token (first 10 chars): {}", jwt != null && jwt.length() > 10 ? jwt.substring(0, 10) + "..." : "null_or_short_token");
        logger.info("Redirecting OAuth2 user to: {}", targetUrl);

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}