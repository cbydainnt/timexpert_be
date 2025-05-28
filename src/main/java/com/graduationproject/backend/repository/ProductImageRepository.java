package com.graduationproject.backend.repository;

import com.graduationproject.backend.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository // Thêm @Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    // Có thể thêm các phương thức query tùy chỉnh nếu cần
}