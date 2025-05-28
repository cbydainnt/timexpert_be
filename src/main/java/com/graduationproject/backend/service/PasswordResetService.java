package com.graduationproject.backend.service;

import com.graduationproject.backend.entity.ResetPasswordToken;
import com.graduationproject.backend.entity.User; // Import User
import com.graduationproject.backend.exception.BadRequestException;
import com.graduationproject.backend.exception.ResourceNotFoundException;
import com.graduationproject.backend.repository.ResetPasswordTokenRepository;
import com.graduationproject.backend.repository.UserRepository; // Import UserRepository
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import Transactional

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    @Autowired
    private ResetPasswordTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository; // Inject UserRepository

    @Autowired
    private UserService userService; // Inject UserService để cập nhật pass

    @Autowired // *** INJECT EmailService ***
    private EmailService emailService;
//    private static final int EXPIRY_MINUTES = 30; // Thời gian hết hạn token (phút)

    @Value("${app.otp.expiry-minutes:10}") // Inject từ config
    private int otpExpiryMinutes;
//    public static final int OTP_EXPIRY_MINUTES = 10; // Giảm thời gian hết hạn OTP
    private static final int OTP_LENGTH = 6; // Độ dài mã OTP

    // Helper tạo mã OTP ngẫu nhiên
    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10)); // Tạo số ngẫu nhiên từ 0-9
        }
        return otp.toString();
    }


    @Transactional
    public String createPasswordResetOtp(String email) {
        // ... (kiểm tra user, xóa token cũ) ...

        String otp = generateOtp();
        ResetPasswordToken otpToken = new ResetPasswordToken();
        otpToken.setEmail(email);
        otpToken.setToken(otp);
        // *** DÙNG BIẾN ĐÃ INJECT ***
        otpToken.setExpiryDate(Timestamp.valueOf(LocalDateTime.now().plusMinutes(otpExpiryMinutes)));

        tokenRepository.save(otpToken);

        try {
            emailService.sendPasswordResetOtpEmail(email, otp);
        } catch (Exception e) {
            System.err.println("OTP generated, but failed to send email for: " + email + " - " + e.getMessage());
            // Có thể in thêm stack trace để debug lỗi mail
            e.printStackTrace(); // Thêm dòng này để xem chi tiết lỗi mail
        }
        return otp;
    }



    @Transactional
    public void resetPasswordWithOtp(String email, String otp, String newPassword) {
        // ... (Tìm token, kiểm tra hết hạn dùng otpExpiryMinutes nếu cần so sánh) ...
        ResetPasswordToken otpToken = tokenRepository.findFirstByEmailOrderByExpiryDateDesc(email)
                .orElseThrow(() -> new BadRequestException("No OTP found or expired. Request new one."));

        if (otpToken.getExpiryDate().before(Timestamp.valueOf(LocalDateTime.now()))) {
            tokenRepository.delete(otpToken);
            throw new BadRequestException("OTP has expired. Please request a new one.");
        }

        // ... (kiểm tra khớp OTP, cập nhật pass, xóa token)
        if (!otpToken.getToken().equals(otp)) {
            throw new BadRequestException("Invalid OTP code.");
        }
        userService.updatePassword(email, newPassword);
        tokenRepository.delete(otpToken);
    }
}