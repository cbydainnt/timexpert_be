package com.graduationproject.backend.service;

import com.graduationproject.backend.dto.RegisterUserDTO;
import com.graduationproject.backend.dto.UserDTO;
import com.graduationproject.backend.entity.User;
import com.graduationproject.backend.entity.enums.AuthProvider;
import com.graduationproject.backend.entity.enums.Role;
import com.graduationproject.backend.exception.BadRequestException;
import com.graduationproject.backend.exception.OperationFailedException;
import com.graduationproject.backend.exception.ResourceNotFoundException;
import com.graduationproject.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import Transactional
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Hàm helper để map User Entity sang UserDTO
    public UserDTO mapToDTO(User user) {
        if (user == null) return null;
        UserDTO dto = new UserDTO();
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhone(user.getPhone());
        dto.setAddress(user.getAddress());
        dto.setDateOfBirth(user.getDateOfBirth());
        if (user.getRole() != null) {
            dto.setRole(user.getRole().name());
        }
        dto.setProvider(user.getProvider().name());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }

    @Transactional // Đảm bảo toàn vẹn dữ liệu
    public User registerUser(RegisterUserDTO registerUserDTO) {
        // Kiểm tra username hoặc email đã tồn tại chưa
        if (userRepository.findByUsername(registerUserDTO.getUsername()).isPresent()) {
            throw new BadRequestException("Username already exists: " + registerUserDTO.getUsername());
        }
        if (userRepository.findByEmail(registerUserDTO.getEmail()).isPresent()) {
            throw new BadRequestException("Email already exists: " + registerUserDTO.getEmail());
        }

        User newUser = new User();
        newUser.setUsername(registerUserDTO.getUsername());
        newUser.setPassword(passwordEncoder.encode(registerUserDTO.getPassword())); // Mã hóa mật khẩu
        newUser.setEmail(registerUserDTO.getEmail());
        newUser.setFirstName(registerUserDTO.getFirstName());
        newUser.setLastName(registerUserDTO.getLastName());
        newUser.setPhone(registerUserDTO.getPhone());
        newUser.setAddress(registerUserDTO.getAddress());
        newUser.setDateOfBirth(registerUserDTO.getDateOfBirth());

        // Thiết lập giá trị mặc định cho đăng ký thông thường
        newUser.setRole(Role.BUYER);
        newUser.setProvider(AuthProvider.LOCAL);

        return userRepository.save(newUser);
    }

    // Giữ nguyên phương thức này để CustomUserDetailsService sử dụng
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    // Giữ nguyên phương thức này để PasswordResetService sử dụng
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // Dùng cho login DTO
    public Optional<User> authenticateUser(String username, String rawPassword) {
        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isPresent() && passwordEncoder.matches(rawPassword, optionalUser.get().getPassword())) {
            return optionalUser;
        }
        return Optional.empty();
    }

    @Transactional
    public UserDTO updateUserProfile(long userId, UserDTO userDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

        // Chỉ cho phép cập nhật các trường nhất định
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setPhone(userDTO.getPhone());
        user.setAddress(userDTO.getAddress());
        user.setDateOfBirth(userDTO.getDateOfBirth());

        // Không cho phép cập nhật username, email, role qua profile update thông thường
        // user.setEmail(userDTO.getEmail()); // Nếu muốn cho cập nhật email cần logic xác thực

        User updatedUser = userRepository.save(user);
        return mapToDTO(updatedUser);
    }

    @Transactional
    public void updatePassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        String encodedPassword1 = passwordEncoder.encode(newPassword);

        user.setPassword(encodedPassword1);

        userRepository.save(user);
    }

    @Transactional // Quan trọng: Đảm bảo toàn vẹn dữ liệu
    public void changePassword(long userId, String currentPassword, String newPassword) {
        // 1. Tìm người dùng bằng userId
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

        // 2. Kiểm tra mật khẩu hiện tại có khớp không
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadRequestException("Incorrect current password.");
        }

        // 3. Kiểm tra mật khẩu mới có trùng mật khẩu cũ không (tùy chọn)
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BadRequestException("New password cannot be the same as the old password.");
        }

        // 4. Mã hóa mật khẩu mới
        String encodedNewPassword = passwordEncoder.encode(newPassword);

        // 5. Cập nhật mật khẩu mới cho user
        user.setPassword(encodedNewPassword);
    }

    @Transactional
    public User processOAuth2User(String registrationId, OAuth2User oauth2User) {
        logger.info("BEGIN processOAuth2User. Registration ID: {}", registrationId);

        String email = oauth2User.getAttribute("email");
        if (!StringUtils.hasText(email)) {
            logger.error("Email not found from OAuth2 provider {}. OAuth2User attributes: {}", registrationId, oauth2User.getAttributes());
            throw new BadRequestException("Email not found from OAuth2 provider " + registrationId);
        }
        logger.info("OAuth2 User Email: {}", email);

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;
        AuthProvider provider;

        try {
            provider = AuthProvider.valueOf(registrationId.toUpperCase());
            logger.info("Provider determined: {}", provider);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid registrationId for AuthProvider: {}. Available providers: {}", registrationId, AuthProvider.values(), e);
            throw new BadRequestException("Invalid OAuth2 provider: " + registrationId);
        }

        if (userOptional.isPresent()) {
            user = userOptional.get();
            logger.info("User found in DB with email {}. Current username: {}, Current provider: {}", email, user.getUsername(), user.getProvider());

            if (!user.getProvider().equals(provider)) {
                logger.warn("User {} (email {}) already registered with provider {} but attempting login with {}.",
                        user.getUsername(), email, user.getProvider(), provider);
                throw new BadRequestException("Email already registered with " + user.getProvider() + ". Please login using that method.");
            }
            logger.info("User {} authenticated with correct provider {}.", user.getUsername(), provider);
            // Cập nhật thông tin nếu cần (tên)
            String name = oauth2User.getAttribute("name");
            String currentFullName = (user.getFirstName() != null ? user.getFirstName() : "") + (user.getLastName() != null ? " " + user.getLastName() : "");
            currentFullName = currentFullName.trim();

            if (StringUtils.hasText(name) && !name.trim().equals(currentFullName)) {
                logger.info("Updating name for user {}. Current name: '{}', New name from OAuth2: '{}'", user.getUsername(), currentFullName, name.trim());
                String[] nameParts = name.trim().split(" ", 2);
                user.setFirstName(nameParts[0]);
                user.setLastName(nameParts.length > 1 ? nameParts[1] : "");
                try {
                    user = userRepository.save(user);
                    logger.info("Successfully updated name for existing user {}. New name: {} {}", user.getUsername(), user.getFirstName(), user.getLastName());
                } catch (Exception e) {
                    logger.error("Failed to update name for existing user {}: {}", user.getUsername(), e.getMessage(), e);
                    // Ghi nhận lỗi nhưng có thể không cần throw, tùy vào yêu cầu
                }
            } else {
                logger.info("Name for user {} is already up-to-date or not provided by OAuth2. Skipping name update.", user.getUsername());
            }
        } else {
            logger.info("User with email {} NOT found in DB. Creating new user with provider {}.", email, provider);

            user = new User();
            user.setProvider(provider);
            user.setEmail(email);
            user.setRole(Role.BUYER);
            user.setCreatedAt(Timestamp.from(Instant.now()));
            logger.info("Initial user object created for email {}: Provider={}, Email={}, Role={}, CreatedAt={}",
                    email, user.getProvider(), user.getEmail(), user.getRole(), user.getCreatedAt());

            // Tạo username
            String usernamePrefix = email.split("@")[0];
            int count = 0;
            String tempUsername = usernamePrefix;
            logger.info("Generating username. Initial prefix: {}", usernamePrefix);
            while (userRepository.existsByUsername(tempUsername)) {
                tempUsername = usernamePrefix + (++count);
                logger.info("Username {} exists, trying next: {}", usernamePrefix + (count-1 > 0 ? (count-1) : ""), tempUsername);
            }
            user.setUsername(tempUsername);
            logger.info("Generated unique username for new user: {}", tempUsername);

            // Xử lý tên
            String name = oauth2User.getAttribute("name");
            if (StringUtils.hasText(name)) {
                String[] nameParts = name.split(" ", 2);
                user.setFirstName(nameParts[0]);
                user.setLastName(nameParts.length > 1 ? nameParts[1] : "");
                logger.info("Set name for new user: FirstName='{}', LastName='{}'", user.getFirstName(), user.getLastName());
            } else {
                logger.warn("Name not found in OAuth2 attributes for email {}. Setting default name.", email);
                user.setFirstName("User"); // Hoặc để trống nếu DB cho phép
                user.setLastName(tempUsername); // Hoặc để trống
            }

            // Mật khẩu này không dùng để đăng nhập OAuth2, chỉ để thỏa mãn constraint DB
            String randomPassword = java.util.UUID.randomUUID().toString();
            user.setPassword(passwordEncoder.encode(randomPassword)); // Nên mã hóa nếu có ý định dùng cho việc khác sau này
            // Hoặc user.setPassword(randomPassword); // Nếu chỉ là placeholder và không bao giờ dùng để login
            logger.info("Set placeholder encoded password for new OAuth2 user {}", user.getUsername());

            logger.info("Final User object before save: {}", user.toString()); // Log toàn bộ user object
            try {
                user = userRepository.save(user);
                logger.info("Successfully SAVED NEW OAuth2 user! ID: {}, Username: {}", user.getUserId(), user.getUsername());
            } catch (DataIntegrityViolationException e) {
                logger.error("DataIntegrityViolationException while saving new user for email {}. Username attempted: {}. Message: {}. Root cause: {}",
                        email, user.getUsername(), e.getMessage(), (e.getCause() != null ? e.getCause().getMessage() : "N/A"), e);
                throw new BadRequestException("Could not create user due to data conflict: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
            } catch (Exception e) {
                logger.error("Generic Exception while saving new user for email {}. Username attempted: {}. Message: {}",
                        email, user.getUsername(), e.getMessage(), e);
                throw new OperationFailedException("Could not save new user from OAuth2: " + e.getMessage(), e);
            }
        }
        logger.info("END processOAuth2User. Returning user: Username='{}', Email='{}'", (user != null ? user.getUsername() : "N/A"), (user != null ? user.getEmail() : "N/A"));
        return user;
    }

    public List<UserDTO> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public UserDTO getUserById(long userId) {
        // Log ID nhận được
        System.out.println("Received userId in UserService.getUserById: " + userId);

        Optional<User> optionalUser = userRepository.findById(userId);

        // Log kết quả tìm kiếm
        if (optionalUser.isPresent()) {
            System.out.println("User with ID " + userId + " found.");
        } else {
            System.out.println("User with ID " + userId + " NOT found in repository.");
        }

        User user = optionalUser
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

        return mapToDTO(user);
    }

    public Page<UserDTO> getUsersForAdmin(int page, int size, String sortBy, String sortDir, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), sortBy));
        Page<User> userPage;

        if (search != null && !search.trim().isEmpty()) {
            userPage = userRepository.searchByNameOrEmail(search.trim(), pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }

        return userPage.map(this::mapToDTO);
    }

    @Transactional
    public void deleteUserById(long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));
        userRepository.delete(user);
    }

    @Transactional
    public UserDTO createUserByAdmin(RegisterUserDTO registerUserDTO, Role role) {
        // Kiểm tra nếu username đã tồn tại
        if (userRepository.existsByUsername(registerUserDTO.getUsername())) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại.");
        }

        // Kiểm tra nếu email đã tồn tại
        if (userRepository.findByEmail(registerUserDTO.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email đã được sử dụng.");
        }

        // Kiểm tra và xác thực role hợp lệ (Role phải là BUYER, ADMIN, hoặc các role khác nếu có)
        Role userRole = Role.BUYER; // Mặc định là BUYER nếu không có role hợp lệ
        if (role != null) {
            try {
                userRole = Role.valueOf(role.name());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Role không hợp lệ.");
            }
        }

        // Tạo mới người dùng
        User user = new User();
        user.setUsername(registerUserDTO.getUsername());
        user.setEmail(registerUserDTO.getEmail());
        user.setPhone(registerUserDTO.getPhone());
        user.setAddress(registerUserDTO.getAddress());
        user.setDateOfBirth(registerUserDTO.getDateOfBirth());
        user.setFirstName(registerUserDTO.getFirstName());
        user.setLastName(registerUserDTO.getLastName());
        user.setProvider(AuthProvider.LOCAL);
        user.setPassword(passwordEncoder.encode(registerUserDTO.getPassword())); // Mã hóa mật khẩu
        user.setRole(userRole); // Gán role được chọn từ tham số
        user.setCreatedAt(Timestamp.from(Instant.now()));

        // Lưu người dùng vào database
        User savedUser = userRepository.save(user);

        // Trả về thông tin người dùng đã được tạo
        return mapToDTO(savedUser); // Hoặc modelMapper.map(savedUser, UserDTO.class)
    }

//    public Page<UserDTO> getUsersByRoleAndSearch(String role, String search, int page, int size, String sortBy, String sortDir) {
//        Role roleEnum;
//        if (role == null) {
//            role = "BUYER"; // Hoặc một giá trị mặc định mà bạn muốn sử dụng
//        }
//        try {
//            roleEnum = Role.valueOf(role.toUpperCase());
//        } catch (IllegalArgumentException e) {
//            throw new BadRequestException("Role không hợp lệ: " + role);
//        }
//
//        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), sortBy));
//        Page<User> userPage;
//
//        if (StringUtils.hasText(search)) {
//            userPage = userRepository.searchByRoleAndNameOrEmail(roleEnum, search.trim(), pageable);
//        } else {
//            userPage = userRepository.findByRole(roleEnum, pageable);
//        }
//
//        return userPage.map(this::mapToDTO);
//    }
public Page<UserDTO> getUsersByRoleAndSearch(String role, String search, int page, int size, String sortBy, String sortDir) {
    // Tạo đối tượng Pageable cho phân trang và sắp xếp
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), sortBy));
    Page<User> userPage;

    // Kiểm tra xem có lọc theo vai trò cụ thể hay không
    boolean filterByRole = StringUtils.hasText(role); // Kiểm tra role có giá trị và không rỗng

    if (filterByRole) {
        // Nếu có lọc theo vai trò, xác định vai trò và thực hiện tìm kiếm/lọc
        Role roleEnum;
        try {
            roleEnum = Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Role không hợp lệ: " + role);
        }

        if (StringUtils.hasText(search)) {
            // Lọc theo vai trò VÀ tìm kiếm
            userPage = userRepository.searchByRoleAndNameOrEmail(roleEnum, search.trim(), pageable);
        } else {
            // Chỉ lọc theo vai trò
            userPage = userRepository.findByRole(roleEnum, pageable);
        }
    } else {
        // Nếu KHÔNG lọc theo vai trò (mặc định hoặc "Tất cả vai trò")
        if (StringUtils.hasText(search)) {
            // Tìm kiếm trên TẤT CẢ người dùng (không phân biệt vai trò)
            // Ta có thể dùng lại phương thức searchByNameOrEmail hiện có
            userPage = userRepository.searchByNameOrEmail(search.trim(), pageable);
        } else {
            // Lấy TẤT CẢ người dùng (không phân biệt vai trò) với phân trang và sắp xếp
            userPage = userRepository.findAll(pageable); // Sử dụng phương thức findAll của JpaRepository
        }
    }

    // Chuyển đổi Page<User> sang Page<UserDTO> và trả về
    return userPage.map(this::mapToDTO);
}

    @Transactional
    public UserDTO updateUserRole(Long userId, String newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));
        user.setRole(Role.valueOf(newRole.toUpperCase()));
        return mapToDTO(userRepository.save(user));
    }
}
