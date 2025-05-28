package com.graduationproject.backend.controller;

import com.graduationproject.backend.dto.ProductDTO;
import com.graduationproject.backend.entity.Product;
import com.graduationproject.backend.entity.User; // Import User
import com.graduationproject.backend.exception.ResourceNotFoundException; // Import
import com.graduationproject.backend.service.ProductService;
import com.graduationproject.backend.service.RecommendationService;
import com.graduationproject.backend.service.UserService; // Import UserService
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // Import
import org.springframework.security.core.context.SecurityContextHolder; // Import
import org.springframework.security.core.userdetails.UserDetails; // Import
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final ProductService productService;
    private final UserService userService; // Inject UserService

    @Autowired
    public RecommendationController(RecommendationService recommendationService,
                                    ProductService productService,
                                    UserService userService) { // Thêm UserService
        this.recommendationService = recommendationService;
        this.productService = productService;
        this.userService = userService;
    }

     // --- Helper lấy User ---
     private User getCurrentAuthenticatedUser() {
         Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
          if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
              throw new SecurityException("User not authenticated.");
         }
         String username = ((UserDetails)authentication.getPrincipal()).getUsername();
         return userService.findByUsername(username)
                 .orElseThrow(() -> new ResourceNotFoundException("User", "username", username + " from token"));
     }
     // --- End Helper ---


    @GetMapping("/me") // Lấy gợi ý cho user đang đăng nhập
    public ResponseEntity<List<ProductDTO>> getMyRecommendations() {
        long userId = getCurrentAuthenticatedUser().getUserId();
        List<Product> recommendedProducts = recommendationService.getRecommendationsForUser(userId);
        List<ProductDTO> dtos = recommendedProducts.stream()
                                    .map(productService::mapToDTO)
                                    .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // Có thể giữ lại endpoint cũ nếu Admin cần xem gợi ý cho user khác
    // @GetMapping("/{userId}") // Cần thêm @PreAuthorize("hasRole('ADMIN')")
    // public ResponseEntity<List<ProductDTO>> getRecommendationsForUser(@PathVariable long userId) { ... }
}