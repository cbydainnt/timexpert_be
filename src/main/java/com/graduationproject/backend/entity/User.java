package com.graduationproject.backend.entity;

import com.graduationproject.backend.entity.enums.AuthProvider;
import com.graduationproject.backend.entity.enums.Role;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.Id;


import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long userId;
  
    @Column(unique = true, nullable = false, length = 50)
    private String username;
  
    @Column(nullable = false)
    private String password; // Mã hóa bằng BCrypt
  
    @Column(unique = true, nullable = false, length = 100)
    private String email;
  
    @Column(length = 50)
    private String firstName;
  
    @Column(length = 50)
    private String lastName;
  
    @Column(length = 15)
    private String phone;
  
    @Column(length = 255)
    private String address;
  
    private LocalDate dateOfBirth;
  
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;  // ADMIN, BUYER
  
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;  // LOCAL, GOOGLE, FACEBOOK
  
    @CreationTimestamp
    private Timestamp createdAt;



}