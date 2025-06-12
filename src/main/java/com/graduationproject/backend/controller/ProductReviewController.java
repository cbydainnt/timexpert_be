package com.graduationproject.backend.controller;

import com.graduationproject.backend.dto.ProductReviewDTO;
import com.graduationproject.backend.dto.ReviewRequestDTO;
import com.graduationproject.backend.entity.User;
import com.graduationproject.backend.service.ProductReviewService;
import com.graduationproject.backend.service.UserService; // Inject UserService để lấy UserID
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // Import PreAuthorize
import org.springframework.security.core.Authentication; // Import
import org.springframework.security.core.context.SecurityContextHolder; // Import
import org.springframework.security.core.userdetails.UserDetails; // Import
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
public class ProductReviewController {

    private final ProductReviewService productReviewService;
    private final UserService userService;

    @Autowired
    public ProductReviewController(ProductReviewService productReviewService, UserService userService) {
        this.productReviewService = productReviewService;
        this.userService = userService;
    }

    // Helper lấy userId từ SecurityContext
    private long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            // Ném lỗi hoặc trả về null tùy ngữ cảnh, nhưng endpoint cần @Authenticated thường sẽ không rơi vào đây
            throw new SecurityException("User not authenticated.");
        }
        String username = ((UserDetails)authentication.getPrincipal()).getUsername();
        User currentUser = userService.findByUsername(username)
                .orElseThrow(() -> new SecurityException("User not found: " + username)); // Hoặc ResourceNotFoundException
        return currentUser.getUserId();
    }

    @PostMapping()
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProductReviewDTO> addProductReview(@Valid @RequestBody ReviewRequestDTO requestDTO) {
        long userId = getCurrentUserId();
        ProductReviewDTO newReview = productReviewService.addReview(userId, requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(newReview);
    }


    @GetMapping("/products/{productId}") // Ví dụ: /api/reviews/products/1?page=0&size=5
    public ResponseEntity<Page<ProductReviewDTO>> getReviewsForProduct(
            @PathVariable int productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("reviewDate").descending()); // Sắp xếp theo ngày mới nhất
        Page<ProductReviewDTO> reviewsPage = productReviewService.getVisibleReviewsByProductId(productId, pageable);
        return ResponseEntity.ok(reviewsPage);
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteMyReview(@PathVariable Long reviewId) {
        long userId = getCurrentUserId();
        productReviewService.deleteUserReview(reviewId, userId); // Gọi hàm service mới
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProductReviewDTO> updateMyReview(@PathVariable Long reviewId, @Valid @RequestBody ReviewRequestDTO requestDTO) {
        long userId = getCurrentUserId();
        // ProductReviewService.updateReview cần thêm tham số userId để kiểm tra quyền
        ProductReviewDTO updatedReview = productReviewService.updateUserReview(reviewId, userId, requestDTO);
        return ResponseEntity.ok(updatedReview);
    }

    @GetMapping("/admin/reviews")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ProductReviewDTO>> getAllReviewsForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "reviewDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<ProductReviewDTO> reviewsPage = productReviewService.getAllReviewsForAdmin(pageable);
        return ResponseEntity.ok(reviewsPage);
    }

    @GetMapping("/admin/products/{productId}/reviews")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ProductReviewDTO>> getAllReviewsForProductByAdmin(
            @PathVariable int productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("reviewDate").descending());
        Page<ProductReviewDTO> reviewsPage = productReviewService.getAllReviewsByProductIdForAdmin(productId, pageable);
        return ResponseEntity.ok(reviewsPage);
    }


    @PutMapping("/admin/reviews/visibility/{reviewId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductReviewDTO> setReviewVisibility(
            @PathVariable Long reviewId,
            @RequestParam boolean isVisible) {
        ProductReviewDTO updatedReview = productReviewService.setReviewVisibility(reviewId, isVisible);
        return ResponseEntity.ok(updatedReview);
    }

    @DeleteMapping("/admin/reviews/{reviewId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteReviewByAdmin(@PathVariable Long reviewId) {
        productReviewService.deleteReviewForAdmin(reviewId); // Gọi hàm service mới cho admin
        return ResponseEntity.noContent().build();
    }
}