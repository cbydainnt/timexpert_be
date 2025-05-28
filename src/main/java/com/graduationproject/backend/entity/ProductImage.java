package com.graduationproject.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "product_images")
@Data
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_image_id")
    private Long productImageId;

    // Quan hệ ManyToOne với Product (LAZY là tốt nhất)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @ToString.Exclude // Tránh vòng lặp toString
    @EqualsAndHashCode.Exclude // Tránh vòng lặp equals/hashCode
    private Product product;

    @Column(name = "image_url", nullable = false, length = 255) // Hoặc @Lob nếu dùng TEXT
    private String imageUrl;

    @Column(name = "is_primary")
    private boolean primary = false; // Mặc định không phải ảnh chính

    @Column(name = "display_order")
    private int displayOrder = 0; // Mặc định thứ tự 0
}