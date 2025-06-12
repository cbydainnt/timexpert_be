package com.graduationproject.backend.repository;

import com.graduationproject.backend.entity.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    // Tìm đánh giá theo productId, có phân trang và sắp xếp theo ngày mới nhất
    @Query("SELECT pr FROM ProductReview pr JOIN FETCH pr.user u WHERE pr.product.productId = :productId ORDER BY pr.reviewDate DESC")
    Page<ProductReview> findByProductProductIdOrderByReviewDateDesc(@Param("productId") int productId, Pageable pageable);

    // Tìm đánh giá cụ thể theo user, product, và order (để kiểm tra đã tồn tại chưa)
    Optional<ProductReview> findByUserUserIdAndProductProductIdAndOrderOrderId(Long userId, int productId, int orderId);

    // Tính điểm đánh giá trung bình cho một sản phẩm
    @Query("SELECT AVG(pr.rating) FROM ProductReview pr WHERE pr.product.productId = :productId")
    Double getAverageRatingByProductId(@Param("productId") int productId);

    // Đếm số lượng đánh giá cho một sản phẩm
    @Query("SELECT COUNT(pr) FROM ProductReview pr WHERE pr.product.productId = :productId")
    Long countByProductProductId(@Param("productId") int productId);

    // Lấy các đánh giá HIỂN THỊ cho một sản phẩm (dùng cho trang chi tiết sản phẩm của người dùng)
    @Query("SELECT pr FROM ProductReview pr JOIN FETCH pr.user u WHERE pr.product.productId = :productId AND pr.visible = true ORDER BY pr.reviewDate DESC")
    Page<ProductReview> findVisibleByProductProductIdOrderByReviewDateDesc(@Param("productId") int productId, Pageable pageable);

    // Lấy TẤT CẢ đánh giá cho một sản phẩm (dùng cho Admin)
    @Query("SELECT pr FROM ProductReview pr JOIN FETCH pr.user u WHERE pr.product.productId = :productId ORDER BY pr.reviewDate DESC")
    Page<ProductReview> findAllByProductProductIdOrderByReviewDateDesc(@Param("productId") int productId, Pageable pageable);

    // Lấy TẤT CẢ đánh giá (phân trang, dùng cho Admin)
    @Query("SELECT pr FROM ProductReview pr JOIN FETCH pr.user u JOIN FETCH pr.product p ORDER BY pr.reviewDate DESC")
    Page<ProductReview> findAllWithUserDetails(Pageable pageable);

    // Tính điểm trung bình CHỈ TỪ CÁC ĐÁNH GIÁ HIỂN THỊ
    @Query("SELECT AVG(pr.rating) FROM ProductReview pr WHERE pr.product.productId = :productId AND pr.visible = true")
    Double getAverageVisibleRatingByProductId(@Param("productId") int productId);

    // Đếm số lượng đánh giá HIỂN THỊ
    @Query("SELECT COUNT(pr) FROM ProductReview pr WHERE pr.product.productId = :productId AND pr.visible = true")
    Long countVisibleByProductProductId(@Param("productId") int productId);
}