package com.graduationproject.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginDTO {
    @NotBlank(message = "{validation.username.notBlank}")
    private String username;

    @NotBlank(message = "{validation.password.notBlank}")
    private String password;

    private boolean rememberMe; // Giữ lại nếu bạn dùng Remember Me với login form truyền thống
}