package com.graduationproject.backend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderItemDTO {
    private int orderItemId;
    private int productId;
    private String productName; // Thêm tên sản phẩm
    private String productImageUrl; // Thêm ảnh sản phẩm (nếu cần)
    private int quantity;
    private BigDecimal price; // Giá tại thời điểm mua
}