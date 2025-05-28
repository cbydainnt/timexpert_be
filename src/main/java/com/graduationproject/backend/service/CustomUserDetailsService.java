package com.graduationproject.backend.service;

import com.graduationproject.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.*; // Giữ UserDetails của Spring
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Thêm Transactional

// Import User entity của bạn
import com.graduationproject.backend.entity.User;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional(readOnly = true) // Đọc dữ liệu nên readOnly=true
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Lấy user entity từ DB
        User user = userRepository.findByUsername(username)
                     .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        // Trả về đối tượng UserDetails của Spring Security
        // Sử dụng roles() thay vì authorities() nếu Role enum của bạn đơn giản
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword()) // Mật khẩu đã mã hóa
                .roles(user.getRole().name()) // Lấy tên của Enum Role (ADMIN, BUYER)
                // .authorities("ROLE_" + user.getRole().name()) // Hoặc cách này nếu bạn cần tiền tố ROLE_
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false) // Có thể thêm trường 'enabled' vào User entity nếu cần
                .build();
    }

//    @Override
//    @Transactional(readOnly = true)
//    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException { // Đổi tên tham số cho rõ
//        // Lấy user entity từ DB bằng email
//        User user = userRepository.findByEmail(email) // TÌM BẰNG EMAIL
//                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
//
//        return org.springframework.security.core.userdetails.User
//                .withUsername(user.getUsername()) // Vẫn dùng username từ DB cho UserDetails của Spring
//                .password(user.getPassword())
//                .roles(user.getRole().name())
//                .build();
//    }

}