package com.graduationproject.backend.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class SelectedItemDTO {
    @Min(value = 1, message = "Product ID must be positive")
    private int productId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;
}