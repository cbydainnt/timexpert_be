package com.graduationproject.backend.service;

import com.graduationproject.backend.entity.Product;
import com.graduationproject.backend.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RecommendationService {

    @Autowired
    private ProductRepository productRepository;

    /**
     * Lấy gợi ý sản phẩm cho người dùng (LOGIC ĐƠN GIẢN CHO ĐỒ ÁN).
     * Trả về 5 sản phẩm mới nhất.
     * Trong thực tế cần các thuật toán phức tạp hơn (collaborative filtering, content-based...).
     */
    public List<Product> getRecommendationsForUser(Long userId) {
        // TODO: Implement more sophisticated recommendation logic if needed
        // Tạm thời trả về 5 sản phẩm mới nhất
        return productRepository.findAll(PageRequest.of(0, 5, Sort.by("createdAt").descending())).getContent();
    }
}