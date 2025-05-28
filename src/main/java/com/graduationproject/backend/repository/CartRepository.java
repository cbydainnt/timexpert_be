package com.graduationproject.backend.repository;

import com.graduationproject.backend.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository; // Thêm @Repository nếu cần

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserId(Long userId);
    void deleteByUserId(Long userId); // Thêm nếu cần xóa cart theo userId trực tiếp
}