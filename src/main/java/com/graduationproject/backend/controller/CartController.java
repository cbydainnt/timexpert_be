// src/main/java/com/graduationproject/backend/controller/CartController.java
package com.graduationproject.backend.controller;

import com.graduationproject.backend.dto.CartDTO;
import com.graduationproject.backend.dto.CartItemInputDTO;
import com.graduationproject.backend.entity.User;
import com.graduationproject.backend.exception.BadRequestException;
import com.graduationproject.backend.exception.ResourceNotFoundException;
import com.graduationproject.backend.service.CartService;
import com.graduationproject.backend.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty; // Import NotEmpty
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Date;
import java.util.List; // Import List
import java.util.Map; // Import Map

@RestController
@RequestMapping("/api/carts")
@Validated
public class CartController {

    @Autowired private CartService cartService;
    @Autowired private UserService userService;

    //Helper
    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Kiểm tra authentication có null không và có được xác thực không
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() instanceof String && "anonymousUser".equals(authentication.getPrincipal())) {
            throw new SecurityException("User not authenticated."); // Hoặc AccessDeniedException
        }
        // Lấy username từ Principal (UserDetails)
        String username;
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            // Xử lý trường hợp principal không phải là UserDetails (ít gặp với cấu hình JWT/OAuth2 thông thường)
            // Có thể ném lỗi hoặc dựa vào cấu hình security của bạn
            throw new SecurityException("Cannot determine username from authentication principal.");
        }

        return userService.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username + " (from token)"));
    }
    private long getCurrentUserId() { return getCurrentAuthenticatedUser().getUserId(); }


    /** Lấy giỏ hàng chi tiết */
    @GetMapping
    public ResponseEntity<CartDTO> getMyCart() {
        long userId = getCurrentUserId();
        // Gọi service trả về CartDTO
        return cartService.getCartByUserIdWithDetails(userId)
                      .map(ResponseEntity::ok)
                      .orElse(ResponseEntity.ok(new CartDTO(userId, Collections.emptyList(), new Date())));
    }

    /** Thêm item vào giỏ */
//    @PostMapping("/items")
//    public ResponseEntity<CartDTO> addToMyCart(@Valid @RequestBody CartItemInputDTO cartItemInputDTO) {
//        long userId = getCurrentUserId();
//        // Service ném lỗi nếu thất bại
//        CartDTO updatedCart = cartService.addItemToCart(userId, cartItemInputDTO.getProductId(), cartItemInputDTO.getQuantity());
//        return ResponseEntity.ok(updatedCart); // Trả về cart đã cập nhật
//    }
    /** Thêm item vào giỏ */
    @PostMapping("/items")
    public ResponseEntity<Void> addToMyCart(@Valid @RequestBody CartItemInputDTO cartItemInputDTO) { // <<< Đổi thành Void
        long userId = getCurrentUserId();
        cartService.addItemToCart(userId, cartItemInputDTO.getProductId(), cartItemInputDTO.getQuantity());
        return ResponseEntity.ok().build(); // <<< Trả về 200 OK
    }

    /** Cập nhật số lượng item */
//    @PutMapping("/items/{productId}")
//    public ResponseEntity<CartDTO> updateMyCartItemQuantity(
//            @PathVariable int productId,
//            @RequestParam @Min(value = 1, message = "Quantity must be at least 1") int quantity) {
//        long userId = getCurrentUserId();
//        CartDTO updatedCart = cartService.updateItemQuantity(userId, productId, quantity);
//        return ResponseEntity.ok(updatedCart); // Trả về cart đã cập nhật
//    }
    /** Cập nhật số lượng item */
    @PutMapping("/items/{productId}")
    public ResponseEntity<Void> updateMyCartItemQuantity( // <<< Đổi thành Void
                                                          @PathVariable int productId,
                                                          @RequestParam @Min(value = 1) int quantity) {
        long userId = getCurrentUserId();
        cartService.updateItemQuantity(userId, productId, quantity);
        return ResponseEntity.ok().build(); // <<< Trả về 200 OK
    }

    /** Xóa một item khỏi giỏ */
    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartDTO> removeFromMyCart(@PathVariable int productId) { // Trả về CartDTO
        long userId = getCurrentUserId();
        CartDTO updatedCart = cartService.removeItemFromCart(userId, productId); // Service trả về DTO rỗng hoặc DTO mới
        return ResponseEntity.ok(updatedCart); // Luôn trả về 200 OK với CartDTO (có thể rỗng)
    }

    /** Xóa một item khỏi giỏ */
//    @DeleteMapping("/items/{productId}")
//    public ResponseEntity<Void> removeFromMyCart(@PathVariable int productId) { // <<< Đổi thành Void
//        long userId = getCurrentUserId();
//        cartService.removeItemFromCart(userId, productId);
//        // Service đã xử lý việc xóa cart nếu rỗng, trả về 204 là phù hợp
//        return ResponseEntity.noContent().build(); // <<< Trả về 204 No Content
//    }

    /** Xóa nhiều item khỏi giỏ (ví dụ: sau khi đặt hàng) */
    @DeleteMapping("/items/batch")
    public ResponseEntity<Void> removeItemsFromMyCart(
            @RequestBody Map<String, List<Integer>> payload // Nhận {"productIds": [1, 2, 3]}
    ) {
         List<Integer> productIds = payload.get("productIds");
         if (productIds == null || productIds.isEmpty()) {
              throw new BadRequestException("Product IDs list cannot be empty for batch removal.");
         }
        long userId = getCurrentUserId();
        cartService.removeItemsFromCartBatch(userId, productIds);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    /** Xóa toàn bộ giỏ hàng */
    @DeleteMapping
    public ResponseEntity<Void> deleteMyCart() {
        long userId = getCurrentUserId();
        cartService.deleteCart(userId);
        return ResponseEntity.noContent().build();
    }
}