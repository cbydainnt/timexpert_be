package com.graduationproject.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

@Entity
@Table(name = "categories")
@Data
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private int categoryId;
  
    @Column(nullable = false, length = 100)
    private String name;
  
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_visible", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean visible = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Timestamp updatedAt;

}
