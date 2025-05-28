// src/main/java/com/graduationproject/backend/service/CustomOAuth2UserService.java
package com.graduationproject.backend.service;

import com.graduationproject.backend.util.CustomUserDetails;
import com.graduationproject.backend.entity.User;
import com.graduationproject.backend.exception.OAuth2AuthenticationProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    @Autowired
    private UserService userService;

    public CustomOAuth2UserService() { // Constructor này đã được gọi, tốt!
        logger.error("!!!!!!!!!!!!!!!!!!!! CustomOAuth2UserService CONSTRUCTOR CALLED !!!!!!!!!!!!!!!!!!!!");
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // DÒNG LOG QUAN TRỌNG NHẤT CẦN XUẤT HIỆN
        logger.error("!!!!!!!!!!!!!!!!!!!! CustomOAuth2UserService: Method loadUser(...) ENTERED. Client: {} !!!!!!!!!!!!!!!!!!!!", userRequest.getClientRegistration().getRegistrationId());

        // // Tạm thời comment lại Exception để chỉ kiểm tra log
        // throw new OAuth2AuthenticationException("INTENTIONAL_TEST_EXCEPTION_FROM_CUSTOM_OAUTH2_USER_SERVICE");

        OAuth2User oauth2User = super.loadUser(userRequest);
        logger.info("CustomOAuth2UserService: Successfully loaded OAuth2User from provider. Email from attributes: {}", String.valueOf(oauth2User.getAttribute("email")));

        try {
            logger.info("CustomOAuth2UserService: Attempting to call userService.processOAuth2User...");
            User user = userService.processOAuth2User(
                    userRequest.getClientRegistration().getRegistrationId(),
                    oauth2User
            );

            if (user == null) {
                logger.error("CustomOAuth2UserService: userService.processOAuth2User returned NULL user object for email: {}", String.valueOf(oauth2User.getAttribute("email")));
                throw new OAuth2AuthenticationProcessingException("Failed to process OAuth2 user: User object is null.");
            }
            logger.info("CustomOAuth2UserService: userService.processOAuth2User returned User ID: {}, Username: {}", user.getUserId(), user.getUsername());

            return new CustomUserDetails(user, oauth2User.getAttributes());

        } catch (Exception ex) {
            // Ghi log đầy đủ cả message và stack trace của exception gốc
            logger.error("CustomOAuth2UserService: Exception during userService.processOAuth2User for {}. Message: {}. Cause: {}",
                    oauth2User.getAttribute("email"), ex.getMessage(), ex.getCause() != null ? ex.getCause().getMessage() : "N/A", ex); // Thêm 'ex' để log stack trace
            throw new OAuth2AuthenticationProcessingException("Error processing OAuth2 user: " + ex.getMessage(), ex);
        }
    }
}