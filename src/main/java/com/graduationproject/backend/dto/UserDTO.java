package com.graduationproject.backend.dto;

import lombok.Data;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserDTO {
    private long userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private LocalDate dateOfBirth;
    private String role; // Giữ lại role để FE biết quyền hạn
    private Timestamp createdAt;
    private String provider;
}