package com.graduationproject.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder // Dùng Builder để dễ tạo đối tượng
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDetailDTO {
    private int productId;
    private int quantity;
    private BigDecimal price; // Giá sản phẩm hiện tại
    private String name;
    private String imageUrl;
    private int stock; // <<< Số lượng tồn kho
}