package com.graduationproject.backend.controller;

import com.graduationproject.backend.dto.OtpPasswordResetDTO; // *** THAY DTO ***
import com.graduationproject.backend.service.PasswordResetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/password")
@Validated
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @Autowired
    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/forgot")
    public ResponseEntity<String> forgotPassword(@RequestParam @Email(message = "Invalid email format") String email) {
        // Gọi service tạo OTP và gửi mail
        String otp = passwordResetService.createPasswordResetOtp(email); // Đổi tên gọi service nếu cần

        // Chỉ trả về thông báo, OTP được gửi qua email
        return ResponseEntity.ok("Password reset OTP has been sent to: " + email);

        // Hoặc trả về OTP để test (không khuyến khích trong production)
        // return ResponseEntity.ok("OTP sent to: " + email + ". OTP (for testing): " + otp);
    }

    @PostMapping("/reset-otp") // *** ĐỔI PATH VÀ PHƯƠNG THỨC CHO RÕ RÀNG ***
    public ResponseEntity<String> resetPasswordWithOtp(@Valid @RequestBody OtpPasswordResetDTO otpPasswordResetDTO) { // *** SỬ DỤNG DTO MỚI ***
        // Service sẽ xử lý việc kiểm tra email, otp, hết hạn và cập nhật mật khẩu
        passwordResetService.resetPasswordWithOtp(
                otpPasswordResetDTO.getEmail(),
                otpPasswordResetDTO.getOtp(),
                otpPasswordResetDTO.getNewPassword()
        ); // *** GỌI PHƯƠNG THỨC SERVICE MỚI ***
        return ResponseEntity.ok("Password has been reset successfully.");
    }
}