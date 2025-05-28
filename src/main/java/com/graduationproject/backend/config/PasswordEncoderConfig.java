// src/main/java/com/graduationproject/backend/config/PasswordEncoderConfig.java
package com.graduationproject.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordEncoderConfig {

    /**
     * Tạo Bean PasswordEncoder duy nhất cho toàn ứng dụng.
     * Các lớp khác (như UserService, SecurityConfig nếu cần) sẽ inject Bean này.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}