package com.graduationproject.backend.repository;

import com.graduationproject.backend.entity.ResetPasswordToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; // Import Param

import java.util.Optional;

public interface ResetPasswordTokenRepository extends JpaRepository<ResetPasswordToken, Long> {

    // Bỏ phương thức findByToken vì không dùng link nữa
    // Optional<ResetPasswordToken> findByToken(String token);

    // Thêm phương thức tìm OTP mới nhất theo email
    Optional<ResetPasswordToken> findFirstByEmailOrderByExpiryDateDesc(String email);

    // Giữ lại deleteByEmail (hoặc dùng query tường minh)
    @Modifying
    @Query("DELETE FROM ResetPasswordToken t WHERE t.email = :email")
    void deleteByEmail(@Param("email") String email);
}