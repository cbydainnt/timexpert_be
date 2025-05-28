package com.graduationproject.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "products")
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int productId; //cái này là int
  
    @Column(nullable = false, length = 100)
    private String name;  // Ví dụ: "Đồng hồ Rolex Submariner"
  
    @Column(columnDefinition = "TEXT")
    private String description; // Mô tả chi tiết sản phẩm
  
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
  
    @Column(nullable = false)
    private int stock;
    
    @Column(length = 100, unique = true)
    private String barcode;  // Mã vạch sản phẩm
  
    // Các thuộc tính đặc thù cho đồng hồ
    @Column(length = 100)
    private String brand;  // Thương hiệu
  
    @Column(length = 100)
    private String model;  // Model
  
    @Column(length = 100)
    private String movement;  // Loại máy (automatic, quartz,...)
  
    @Column(length = 100)
    private String caseMaterial;  // Chất liệu vỏ
  
    @Column(length = 100)
    private String strapMaterial;  // Chất liệu dây
       
    @Column(length = 50)
    private String dialColor;  // Màu mặt số
  
    @Column(length = 50)
    private String waterResistance;  // Ví dụ: "50m", "100m"
  
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Category category;
  
    @CreationTimestamp
    private Timestamp createdAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @OrderBy("displayOrder ASC, is_primary DESC") // Sắp xếp ảnh theo thứ tự, ảnh chính lên đầu
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ProductImage> images = new ArrayList<>();

    // --- Helper method để lấy URL ảnh chính ---
    // Cách này cần đảm bảo collection `images` đã được load (trong transaction)
    public String getPrimaryImageUrl() {
        if (images == null || images.isEmpty()) {
            return null; // Hoặc trả về URL placeholder mặc định
        }
        // Tìm ảnh có isPrimary = true
        Optional<ProductImage> primary = images.stream().filter(ProductImage::isPrimary).findFirst();
        if (primary.isPresent()) {
            return primary.get().getImageUrl();
        }
        // Nếu không có ảnh nào primary, lấy ảnh đầu tiên theo displayOrder
        return images.get(0).getImageUrl();
    }
}
