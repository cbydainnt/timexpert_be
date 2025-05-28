package com.graduationproject.backend.dto;

import com.graduationproject.backend.entity.enums.AuthProvider;
import com.graduationproject.backend.entity.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterUserDTO {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String password;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @Size(max = 50, message = "First name must be less than 50 characters")
    private String firstName;

    @Size(max = 50, message = "Last name must be less than 50 characters")
    private String lastName;

    @Size(max = 15, message = "Phone number must be less than 15 characters")
    private String phone;

    @Size(max = 255, message = "Address must be less than 255 characters")
    private String address;

    private LocalDate dateOfBirth;

    // Các trường này thường không do người dùng nhập khi đăng ký LOCAL
    // @NotNull(message = "Role is required")
    // private Role role = Role.BUYER; // Mặc định là BUYER khi đăng ký

    // @NotNull(message = "Provider is required")
    // private AuthProvider provider = AuthProvider.LOCAL; // Mặc định là LOCAL
}