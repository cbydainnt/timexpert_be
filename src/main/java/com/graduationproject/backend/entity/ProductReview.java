// src/main/java/com/graduationproject/backend/entity/ProductReview.java
package com.graduationproject.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.sql.Timestamp;

@Entity
// Cập nhật @Table với uniqueConstraints
@Table(name = "product_reviews",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "product_id", "order_id"})
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(nullable = false)
    private int rating; // 1 to 5

    @Lob
    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @CreationTimestamp
    @Column(name = "review_date", updatable = false)
    private Timestamp reviewDate;

    @Column(name = "is_visible", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean visible = true;
}