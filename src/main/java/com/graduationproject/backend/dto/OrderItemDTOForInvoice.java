package com.graduationproject.backend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderItemDTOForInvoice {
    private String productName;
    private int quantity;
    private BigDecimal price; // Giá tại thời điểm mua (price của OrderItem)
    private String productSku; // Hoặc mã sản phẩm (product.getBarcode() chẳng hạn)
    // Không cần productImageUrl cho hóa đơn
}