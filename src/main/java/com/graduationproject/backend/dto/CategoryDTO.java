package com.graduationproject.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryDTO {
    private int categoryId;

    @NotBlank(message = "Category name cannot be blank")
    private String name;

    private String description;
}