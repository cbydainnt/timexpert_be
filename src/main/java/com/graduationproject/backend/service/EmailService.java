package com.graduationproject.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;


    @Value("${app.otp.expiry-minutes:10}") // Inject thời gian hết hạn
    private int otpExpiryMinutesValue;

    /**
     * Gửi email chứa mã OTP reset mật khẩu.
     * @param toEmail Địa chỉ email nhận
     * @param otp     Mã OTP (6 chữ số)
     */
    // *** ĐỔI TÊN THAM SỐ THÀNH OTP ***
    public void sendPasswordResetOtpEmail(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(toEmail);
            message.setSubject("Mã OTP đặt lại mật khẩu của bạn"); // Đổi Subject

            // *** THAY ĐỔI NỘI DUNG EMAIL ĐỂ GỬI OTP ***
            String emailBody = String.format(
                    "Xin chào,\n\n" +
                            "Bạn đã yêu cầu đặt lại mật khẩu.\n\n" +
                            "Mật khẩu một lần (OTP) của bạn là: %s\n\n" + // Hiển thị OTP
                            "Mã này sẽ hết hạn sau %d phút.\n" + // Lấy thời gian từ Service hoặc Config
                            "Vui lòng nhập mã này vào trang đặt lại mật khẩu.\n\n" +
                            "Nếu bạn không yêu cầu, vui lòng bỏ qua email này.\n\n" +
                            "Cảm ơn,\nTimeXpertStore",
                    otp, // Truyền mã OTP vào nội dung
                    otpExpiryMinutesValue // Lấy thời gian hết hạn (cần làm hằng số này public hoặc lấy từ config)
            );
            message.setText(emailBody);
            javaMailSender.send(message);
            System.out.println("Password reset OTP email sent successfully to " + toEmail);
        } catch (MailException e) {
            System.err.println("Error sending password reset OTP email to " + toEmail + ": ");
            e.printStackTrace(); // *** IN STACK TRACE ĐỂ XEM LỖI CHI TIẾT ***
        }
    }

}

