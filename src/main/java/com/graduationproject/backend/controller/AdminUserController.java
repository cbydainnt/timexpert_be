package com.graduationproject.backend.controller;

import com.graduationproject.backend.dto.OrderDTO;
import com.graduationproject.backend.dto.RegisterUserDTO;
import com.graduationproject.backend.dto.UserDTO;
import com.graduationproject.backend.entity.enums.Role;
import com.graduationproject.backend.service.OrderService;
import com.graduationproject.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;


@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    @Autowired
    private UserService userService;

    @Autowired
    private OrderService orderService;
    /**
     * Lấy danh sách toàn bộ người dùng trong hệ thống
     * Chỉ dành cho ADMIN
     */
//    @GetMapping
//    public ResponseEntity<List<UserDTO>> getAllUsers() {
//        List<UserDTO> users = userService.getAllUsers();
//        return ResponseEntity.ok(users);
//    }
    @GetMapping
    public ResponseEntity<Page<UserDTO>> getUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "userId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Page<UserDTO> users = userService.getUsersByRoleAndSearch(
                role != null && !role.name().isEmpty()? role.name() : null,
                search, page, size, sortBy, sortDir
        );
        return ResponseEntity.ok(users);
    }

    /**
     * Lấy thông tin chi tiết của 1 user theo ID
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable long userId) {
        UserDTO user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    /**
     * Cập nhật thông tin người dùng (do Admin cập nhật)
     * Tuỳ bạn có cho cập nhật vai trò hay không
     */
    @PutMapping("/{userId}")
    public ResponseEntity<UserDTO> updateUserByAdmin(@PathVariable long userId, @RequestBody UserDTO userDTO) {
        UserDTO updatedUser = userService.updateUserProfile(userId, userDTO); // hoặc viết hàm riêng cho admin nếu cần
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Xoá user (nếu có nhu cầu)
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUserById(userId); // bạn cần thêm hàm này nếu chưa có
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/orders")
    public ResponseEntity<Page<OrderDTO>> getUserOrdersForAdmin(
            @PathVariable long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortField = "createdAt";
        if (sortBy.equalsIgnoreCase("orderId") || sortBy.equalsIgnoreCase("totalAmount") || sortBy.equalsIgnoreCase("status") || sortBy.equalsIgnoreCase("createdAt")) {
            sortField = sortBy;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        Page<OrderDTO> userOrdersPage = orderService.findOrdersByUserId(userId, pageable);
        return ResponseEntity.ok(userOrdersPage);
    }

    @PostMapping("/create")
    public ResponseEntity<UserDTO> createUserByAdmin(
            @RequestBody RegisterUserDTO registerUserDTO,
            @RequestParam(required = false) Role role) {
        if (role == null) {
            role = Role.BUYER;  // Gán role mặc định nếu không có
        }

        if (!Arrays.asList(Role.values()).contains(role)) {
            return ResponseEntity.badRequest().body(null);  // Trả về lỗi nếu role không hợp lệ
        }

        UserDTO userDTO = userService.createUserByAdmin(registerUserDTO, role);
        return ResponseEntity.status(HttpStatus.CREATED).body(userDTO);
    }


}
