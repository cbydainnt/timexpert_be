package com.graduationproject.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

@Entity
@Table(name = "cart_items", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"cart_id", "product_id"}) // Đảm bảo unique
})
@Data
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_item_id")
    private Long cartItemId;

    // Quan hệ ManyToOne với Cart
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Cart cart;

    // Chỉ lưu productId thay vì ManyToOne với Product để tránh load không cần thiết
    // Thông tin product sẽ được lấy qua ProductService khi cần
     @Column(name = "product_id", nullable = false)
     private Integer productId;
    // Hoặc nếu muốn dùng ManyToOne:
    // @ManyToOne(fetch = FetchType.LAZY, optional = false)
    // @JoinColumn(name = "product_id", nullable = false)
    // @ToString.Exclude
    // @EqualsAndHashCode.Exclude
    // private Product product;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "price_at_addition", precision = 10, scale = 2, nullable = false)
    private BigDecimal priceAtAddition; // Giá tại thời điểm thêm vào giỏ

}