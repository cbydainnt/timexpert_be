package com.graduationproject.backend.controller;

import com.graduationproject.backend.config.JwtTokenProvider;
import com.graduationproject.backend.dto.ChangePasswordRequestDTO;
import com.graduationproject.backend.dto.LoginDTO;
import com.graduationproject.backend.dto.RegisterUserDTO; // Import Register DTO
import com.graduationproject.backend.dto.UserDTO; // Import User DTO
import com.graduationproject.backend.entity.User;
import com.graduationproject.backend.exception.ResourceNotFoundException;
import com.graduationproject.backend.service.UserService;
import jakarta.validation.Valid; // Import Valid
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.Authentication; // Import Authentication
import org.springframework.security.core.context.SecurityContextHolder; // Import SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails; // Import UserDetails
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    // Sử dụng final với constructor injection là best practice
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public UserController(UserService userService, JwtTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            // Ném lỗi hoặc trả về null tùy ngữ cảnh, nhưng endpoint cần @Authenticated thường sẽ không rơi vào đây
            throw new SecurityException("User not authenticated.");
        }
        String username = ((UserDetails)authentication.getPrincipal()).getUsername();
        // Cần inject UserService để lấy User entity đầy đủ
        // Giả sử userService đã được inject
        return userService.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username + " from token"));
    }

    private long getCurrentUserId() {
        return getCurrentAuthenticatedUser().getUserId();
    }
    @PostMapping("/register")
    // Nhận vào RegisterUserDTO và dùng @Valid
    public ResponseEntity<UserDTO> register(@Valid @RequestBody RegisterUserDTO registerUserDTO) {
        User registeredUser = userService.registerUser(registerUserDTO);
        // Trả về UserDTO thay vì User entity
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.mapToDTO(registeredUser));
    }

    @PostMapping("/login")
    // Nhận vào LoginDTO và dùng @Valid
    public ResponseEntity<?> login(@Valid @RequestBody LoginDTO loginDTO) {
        Optional<User> userOpt = userService.authenticateUser(loginDTO.getUsername(), loginDTO.getPassword());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String token = jwtTokenProvider.generateToken(user.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            // Trả về UserDTO thay vì User entity
            response.put("user", userService.mapToDTO(user));
            return ResponseEntity.ok(response);
        } else {
             // Không cần trả về message body khi là 401 Unauthorized chuẩn
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
             // Hoặc nếu muốn trả message:
             // return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }
    }

     // Endpoint lấy thông tin user đang đăng nhập (profile)
     @GetMapping("/profile")
     public ResponseEntity<UserDTO> getCurrentUserProfile() {
         Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
         if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
              return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
         }
         String username;
         Object principal = authentication.getPrincipal();

         if (principal instanceof UserDetails) {
             username = ((UserDetails)principal).getUsername();
         } else if (principal instanceof String) {
              username = (String) principal;
         } else {
             //
             username = null;
             // Không lấy được username từ principal
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
         }


         User user = userService.findByUsername(username)
                 .orElseThrow(() -> new ResourceNotFoundException("User", "username", username)); // Nên có lỗi này

         return ResponseEntity.ok(userService.mapToDTO(user));
     }

     // Endpoint cập nhật profile user đang đăng nhập
     @PutMapping("/profile")
     public ResponseEntity<UserDTO> updateCurrentUserProfile(@Valid @RequestBody UserDTO userDTO) {
          Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
         if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
              return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
         }
          String username = ((UserDetails)authentication.getPrincipal()).getUsername();

          User currentUser = userService.findByUsername(username)
                   .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

          // Gọi service để cập nhật, chỉ truyền ID và DTO chứa các trường cần cập nhật
          UserDTO updatedUserDTO = userService.updateUserProfile(currentUser.getUserId(), userDTO);
          return ResponseEntity.ok(updatedUserDTO);
     }

    /**
     * Endpoint cho người dùng đang đăng nhập thay đổi mật khẩu của chính họ.
     * Yêu cầu xác thực (JWT).
     * @param request DTO chứa currentPassword và newPassword
     * @return ResponseEntity báo thành công hoặc lỗi.
     */
    @PutMapping("/profile/change-password") // Hoặc dùng @PostMapping tùy ý nghĩa RESTful bạn muốn
    public ResponseEntity<String> changeUserPassword(
            @Valid @RequestBody ChangePasswordRequestDTO request
    ) {
        // Lấy thông tin người dùng đang đăng nhập từ security context
        User currentUser = getCurrentAuthenticatedUser();
        long userId = currentUser.getUserId();

        // Gọi service để thực hiện đổi mật khẩu
        userService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());

        // Trả về thông báo thành công
        return ResponseEntity.ok("Password changed successfully.");
    }
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        User user = userService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return ResponseEntity.ok(userService.mapToDTO(user));
    }

}