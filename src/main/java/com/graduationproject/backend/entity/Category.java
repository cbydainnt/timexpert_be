package com.graduationproject.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

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
}
