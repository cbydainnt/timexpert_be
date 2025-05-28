package com.graduationproject.backend.controller;

import com.graduationproject.backend.dto.FavoriteDTO; // Import DTO
import com.graduationproject.backend.entity.User; // Import User
import com.graduationproject.backend.exception.ResourceNotFoundException; // Import
// import com.graduationproject.backend.entity.Favorite; // Không trả về entity nữa
import com.graduationproject.backend.service.FavoriteService;
import com.graduationproject.backend.service.UserService; // Import UserService
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.Authentication; // Import
import org.springframework.security.core.context.SecurityContextHolder; // Import
import org.springframework.security.core.userdetails.UserDetails; // Import
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final UserService userService; // Inject UserService để lấy user từ principal

    @Autowired
    public FavoriteController(FavoriteService favoriteService, UserService userService) {
        this.favoriteService = favoriteService;
        this.userService = userService;
    }

     // Helper lấy userId từ SecurityContext
     private long getCurrentUserId() {
          Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
          if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
              throw new ResourceNotFoundException("User", "principal", "anonymous"); // Hoặc trả về lỗi 401 tùy cách xử lý
         }
          String username = ((UserDetails)authentication.getPrincipal()).getUsername();
           User currentUser = userService.findByUsername(username)
                   .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
           return currentUser.getUserId();
     }


    @GetMapping // Lấy danh sách yêu thích của user đang đăng nhập
    public ResponseEntity<List<FavoriteDTO>> getCurrentUserFavorites() {
        long userId = getCurrentUserId();
        List<FavoriteDTO> favorites = favoriteService.getFavorites(userId);
        return ResponseEntity.ok(favorites);
    }

    @PostMapping // Thêm sản phẩm vào danh sách yêu thích của user đang đăng nhập
    public ResponseEntity<FavoriteDTO> addFavorite(@RequestParam int productId) {
        long userId = getCurrentUserId();
        FavoriteDTO favoriteDTO = favoriteService.addFavorite(userId, productId);
        // Service đã xử lý nếu đã tồn tại hoặc có lỗi
        return ResponseEntity.status(HttpStatus.CREATED).body(favoriteDTO);
    }

    @DeleteMapping("/{favoriteId}") // Xóa theo ID của bản ghi Favorite
    public ResponseEntity<Void> removeFavorite(@PathVariable long favoriteId) {
        long userId = getCurrentUserId();
        // Service sẽ kiểm tra favoriteId có thuộc user đang đăng nhập không
        favoriteService.removeFavorite(userId, favoriteId);
        return ResponseEntity.noContent().build();
    }

     // Endpoint thay thế: Xóa theo Product ID
     @DeleteMapping("/product/{productId}")
     public ResponseEntity<Void> removeFavoriteByProductId(@PathVariable int productId) {
         long userId = getCurrentUserId();
         favoriteService.removeFavoriteByProduct(userId, productId);
         return ResponseEntity.noContent().build();
     }
}