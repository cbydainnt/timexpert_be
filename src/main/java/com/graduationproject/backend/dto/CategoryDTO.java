package com.graduationproject.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.sql.Timestamp;

@Data
public class CategoryDTO {
    private int categoryId;

    @NotBlank(message = "Category name cannot be blank")
    private String name;

    private String description;

    private boolean visible;

    private Timestamp updatedAt;
}