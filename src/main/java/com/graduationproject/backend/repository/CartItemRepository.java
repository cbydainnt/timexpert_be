// src/main/java/com/graduationproject/backend/repository/CartItemRepository.java
package com.graduationproject.backend.repository;

import com.graduationproject.backend.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /** Tìm CartItem cụ thể */
    Optional<CartItem> findByCartCartIdAndProductId(Long cartId, Integer productId);

    List<CartItem> findByCartCartId(Long cartId);
    /** Xóa nhiều CartItem theo cartId và danh sách productId */
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.cartId = :cartId AND ci.productId IN :productIds")
    int deleteByCartCartIdAndProductIdIn(@Param("cartId") Long cartId, @Param("productIds") List<Integer> productIds);
    // Trả về int (số dòng đã xóa)

    /** Xóa một CartItem theo cartId và productId (Cách khác thay cho delete(entity)) */
    @Modifying
    int deleteByCartCartIdAndProductId(Long cartId, Integer productId);
    // Trả về int (số dòng đã xóa, thường là 1 hoặc 0)

    /** Đếm số item còn lại trong cart (Cách khác thay cho load lại Cart) */
    long countByCartCartId(Long cartId);

}