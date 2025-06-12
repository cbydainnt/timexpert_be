package com.graduationproject.backend.service;

import com.graduationproject.backend.dto.ProductReviewDTO;
import com.graduationproject.backend.dto.ReviewRequestDTO;
import com.graduationproject.backend.entity.Order;
import com.graduationproject.backend.entity.OrderItem;
import com.graduationproject.backend.entity.Product;
import com.graduationproject.backend.entity.ProductReview;
import com.graduationproject.backend.entity.User;
import com.graduationproject.backend.entity.enums.OrderStatus;
import com.graduationproject.backend.exception.BadRequestException;
import com.graduationproject.backend.exception.ResourceNotFoundException;
import com.graduationproject.backend.repository.ProductReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
@Service
public class ProductReviewService {

    private static final Logger logger = LoggerFactory.getLogger(ProductReviewService.class);

    @Autowired
    private ProductReviewRepository productReviewRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private ProductService productService;
    @Autowired
    private OrderService orderService;

    // ... (mapToDTO giữ nguyên) ...
//    public ProductReviewDTO mapToDTO(ProductReview review) {
//        if (review == null) return null;
//        return ProductReviewDTO.builder()
//                .reviewId(review.getReviewId())
//                .rating(review.getRating())
//                .comment(review.getComment())
//                .reviewDate(review.getReviewDate())
//                .productId(review.getProduct() != null ? review.getProduct().getProductId() : 0)
//                .userId(review.getUser() != null ? review.getUser().getUserId() : 0L)
//                .userFirstName(review.getUser() != null ? review.getUser().getFirstName() : null)
//                .userLastName(review.getUser() != null ? review.getUser().getLastName() : null)
//                .visible(review.isVisible())
//                .build();
//    }
    public ProductReviewDTO mapToDTO(ProductReview review) {
        if (review == null) return null;
        return ProductReviewDTO.builder()
                .reviewId(review.getReviewId())
                .rating(review.getRating())
                .comment(review.getComment())
                .reviewDate(review.getReviewDate())
                .productId(review.getProduct() != null ? review.getProduct().getProductId() : 0)
                .productName(review.getProduct() != null ? review.getProduct().getName() : "N/A") // << LẤY TÊN SẢN PHẨM
                .userId(review.getUser() != null ? review.getUser().getUserId() : 0L)
                .userFirstName(review.getUser() != null ? review.getUser().getFirstName() : null)
                .userLastName(review.getUser() != null ? review.getUser().getLastName() : null)
                .visible(review.isVisible())
                .build();
    }

    @Transactional
    public ProductReviewDTO addReview(Long userId, ReviewRequestDTO requestDTO) {
        // 1. Tìm User và Product entities
        User user = userService.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "ID", userId));
        Product product = productService.findProductEntityById(requestDTO.getProductId());

        // Bước mới: Tìm một OrderId hợp lệ để gán cho review
        // Phương thức này cần được tạo trong OrderService
        List<Order> completedOrdersWithProduct = orderService.findCompletedOrdersByUserAndProduct(userId, product.getProductId());

        Order eligibleOrder = null;
        for (Order completedOrder : completedOrdersWithProduct) {
            boolean alreadyReviewedForThisOrder = productReviewRepository
                    // Sử dụng completedOrder.getOrderId() ở đây để kiểm tra
                    .findByUserUserIdAndProductProductIdAndOrderOrderId(userId, product.getProductId(), completedOrder.getOrderId())
                    .isPresent();
            if (!alreadyReviewedForThisOrder) {
                eligibleOrder = completedOrder;
                logger.info("Found eligible Order ID {} for review for product ID {} by user ID {}", eligibleOrder.getOrderId(), product.getProductId(), userId);
                break; // Tìm thấy một đơn hàng hợp lệ để đánh giá
            }
        }

        if (eligibleOrder == null) {
            if (completedOrdersWithProduct.isEmpty()) {
                logger.warn("User {} attempted to review product ID {} but has no completed orders with this product.", userId, product.getProductId());
                throw new BadRequestException("Bạn cần mua và hoàn thành đơn hàng chứa sản phẩm này trước khi đánh giá.");
            } else {
                logger.warn("User {} attempted to review product ID {} but has already reviewed it for all eligible completed orders.", userId, product.getProductId());
                throw new BadRequestException("Bạn đã đánh giá sản phẩm này cho tất cả các đơn hàng hợp lệ của mình.");
            }
        }

        // Các kiểm tra khác (như đơn hàng có thuộc user, trạng thái COMPLETED, sản phẩm trong đơn)
        // đã được bao hàm trong logic tìm eligibleOrder (vì findCompletedOrdersByUserAndProduct chỉ trả về đơn COMPLETED của user đó).
        // Chỉ cần đảm bảo productInOrder nếu logic tìm eligibleOrder không quá chặt chẽ về sản phẩm cụ thể trong orderItem (mặc dù nên có).
        // Hiện tại, findCompletedOrdersByUserAndProduct đã ngầm kiểm tra sản phẩm trong đơn.

        // 6. Tạo và lưu đánh giá
        ProductReview newReview = new ProductReview();
        newReview.setUser(user);
        newReview.setProduct(product);
        newReview.setOrder(eligibleOrder); // <<==== GÁN eligibleOrder TÌM ĐƯỢC
        newReview.setRating(requestDTO.getRating());
        newReview.setComment(requestDTO.getComment());
        newReview.setVisible(true);

        ProductReview savedReview = productReviewRepository.save(newReview);
        logger.info("New review added: Review ID {}, Product ID {}, User ID {}, associated with Order ID {}",
                savedReview.getReviewId(),
                savedReview.getProduct().getProductId(),
                savedReview.getUser().getUserId(),
                savedReview.getOrder().getOrderId());

        // 7. Cập nhật điểm đánh giá trung bình và số lượng đánh giá cho sản phẩm
        updateProductAverageRatingAndCount(requestDTO.getProductId());

        return mapToDTO(savedReview);
    }

    // ... (các phương thức khác như getVisibleReviewsByProductId, getAllReviewsByProductIdForAdmin, v.v... giữ nguyên)

    @Transactional(readOnly = true)
    public Page<ProductReviewDTO> getVisibleReviewsByProductId(int productId, Pageable pageable) {
        productService.findProductEntityById(productId);
        Page<ProductReview> reviewsPage = productReviewRepository.findVisibleByProductProductIdOrderByReviewDateDesc(productId, pageable);
        return reviewsPage.map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public Page<ProductReviewDTO> getAllReviewsByProductIdForAdmin(int productId, Pageable pageable) {
        productService.findProductEntityById(productId);
        Page<ProductReview> reviewsPage = productReviewRepository.findAllByProductProductIdOrderByReviewDateDesc(productId, pageable);
        return reviewsPage.map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public Page<ProductReviewDTO> getAllReviewsForAdmin(Pageable pageable) {
        Page<ProductReview> reviewsPage = productReviewRepository.findAllWithUserDetails(pageable);
        return reviewsPage.map(this::mapToDTO);
    }

    @Transactional
    public void updateProductAverageRatingAndCount(int productId) {
        Double avgRatingDouble = productReviewRepository.getAverageVisibleRatingByProductId(productId);
        Long reviewCount = productReviewRepository.countVisibleByProductProductId(productId);

        Product product = productService.findProductEntityById(productId);

        product.setAverageRating(avgRatingDouble != null ? BigDecimal.valueOf(avgRatingDouble).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)); // Sử dụng HALF_UP hoặc một RoundingMode phù hợp
        product.setReviewCount(reviewCount != null ? reviewCount.intValue() : 0);

        // productRepository.save(product); // Không cần thiết nếu product là managed và phương thức này @Transactional
        logger.info("Updated Product ID {} with VISIBLE average rating: {} and VISIBLE review count: {}", productId, product.getAverageRating(), product.getReviewCount());
    }

    @Transactional(readOnly = true)
    public ProductReviewDTO getReviewById(Long reviewId) {
        ProductReview review = productReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductReview", "reviewId", reviewId));
        return mapToDTO(review);
    }

    @Transactional
    public void deleteReviewForAdmin(Long reviewId) {
        ProductReview review = productReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductReview", "reviewId", reviewId));
        int productId = review.getProduct().getProductId();
        productReviewRepository.delete(review);
        logger.info("ADMIN action: Review ID {} deleted for Product ID {}", reviewId, productId);
        updateProductAverageRatingAndCount(productId);
    }

    // Đổi tên hàm này để user tự xóa review của họ
    @Transactional
    public void deleteUserReview(Long reviewId, Long userId) { // << Thêm userId để kiểm tra quyền
        ProductReview review = productReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductReview", "reviewId", reviewId));

        // Kiểm tra xem người dùng hiện tại có phải là chủ của đánh giá này không
        if (!Objects.equals(review.getUser().getUserId(), userId)) {
            throw new AccessDeniedException("Bạn không có quyền xóa đánh giá này.");
        }

        int productId = review.getProduct().getProductId();
        productReviewRepository.delete(review);
        logger.info("User {} deleted their review ID {} for Product ID {}", userId, reviewId, productId);
        updateProductAverageRatingAndCount(productId);
    }

    // Đổi tên hàm này để user tự cập nhật review của họ
    @Transactional
    public ProductReviewDTO updateUserReview(Long reviewId, Long userId, ReviewRequestDTO requestDTO) { // << Thêm userId
        ProductReview review = productReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductReview", "reviewId", reviewId));

        // Kiểm tra xem người dùng hiện tại có phải là chủ của đánh giá này không
        if (!Objects.equals(review.getUser().getUserId(), userId)) {
            throw new AccessDeniedException("Bạn không có quyền cập nhật đánh giá này.");
        }
        // Kiểm tra xem sản phẩm có khớp không (an toàn hơn, mặc dù requestDTO đã có productId)
        if (review.getProduct().getProductId() != requestDTO.getProductId()) {
            throw new BadRequestException("Không thể thay đổi đánh giá cho một sản phẩm khác.");
        }
        // Kiểm tra xem orderId có khớp không (nếu ReviewRequestDTO có orderId)
        // Hoặc không cho phép thay đổi orderId liên kết với review
        if (requestDTO.getOrderId() != null && review.getOrder().getOrderId() != requestDTO.getOrderId()){
            throw new BadRequestException("Không thể thay đổi đơn hàng liên kết với đánh giá này.");
        }


        review.setRating(requestDTO.getRating());
        review.setComment(requestDTO.getComment());
        // Không cho phép cập nhật visible ở đây, đó là việc của Admin
        // review.setVisible(...);

        ProductReview updatedReview = productReviewRepository.save(review);
        logger.info("User {} updated their review ID {} for Product ID {}", userId, updatedReview.getReviewId(), updatedReview.getProduct().getProductId());
        updateProductAverageRatingAndCount(review.getProduct().getProductId());
        return mapToDTO(updatedReview);
    }

    @Transactional
    public ProductReviewDTO setReviewVisibility(Long reviewId, boolean isVisible) {
        ProductReview review = productReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductReview", "reviewId", reviewId));
        boolean oldVisibility = review.isVisible();
        review.setVisible(isVisible);
        ProductReview updatedReview = productReviewRepository.save(review);
        logger.info("ADMIN action: Review ID {} visibility changed from {} to {}.",
                updatedReview.getReviewId(), oldVisibility, updatedReview.isVisible());
        updateProductAverageRatingAndCount(review.getProduct().getProductId());
        return mapToDTO(updatedReview);
    }
}

