package com.graduationproject.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

@Data
public class ProductDTO {
    private int productId;
    private String name;
    private String description;
    private BigDecimal price;
    private int stock;
    private String primaryImageUrl;
    private List<String> imageUrls;
    private String barcode;
    private String brand;
    private String model;
    private String movement;
    private String caseMaterial;
    private String strapMaterial;
    private String dialColor;
    private String waterResistance;
    private int categoryId;
    private String categoryName;
    private Timestamp createdAt;
}