package com.graduationproject.backend.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemInputDTO { // Đổi tên để phân biệt với CartItem entity/trong Cart DTO
    @Min(value = 1, message = "Product ID must be positive")
    private int productId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;

    // Price thường lấy từ server, không nên để client gửi lên khi add to cart
    // private BigDecimal price;
}