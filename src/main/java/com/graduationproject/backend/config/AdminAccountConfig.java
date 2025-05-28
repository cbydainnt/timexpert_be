//package com.graduationproject.backend.config;
//
//
//import com.graduationproject.backend.entity.User;
//import com.graduationproject.backend.entity.enums.AuthProvider;
//import com.graduationproject.backend.entity.enums.Role;
//import com.graduationproject.backend.repository.UserRepository;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//@Configuration
//public class AdminAccountConfig {
//
//    @Bean
//    public CommandLineRunner createAdminAccount(UserRepository userRepository, PasswordEncoder passwordEncoder) {
//        return args -> {
//            // Kiểm tra nếu chưa có tài khoản admin nào
//            if (userRepository.findByUsername("admin").isEmpty()) {
//                User admin = new User();
//                admin.setUsername("admin");
//                // Mã hóa mật khẩu trước khi lưu
//                admin.setPassword(passwordEncoder.encode("admin123"));
//                admin.setEmail("thanhpc.works@gmail.com");
//                admin.setRole(Role.ADMIN);
//                admin.setProvider(AuthProvider.LOCAL);
//                admin.setFirstName("Phùng");
//                admin.setLastName("Thành");
//                userRepository.save(admin);
//                System.out.println("Tài khoản admin đã được tạo.");
//            } else {
//                System.out.println("Tài khoản admin đã tồn tại.");
//            }
//        };
//    }
//}
