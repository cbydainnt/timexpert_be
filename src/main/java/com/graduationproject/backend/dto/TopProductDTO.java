//DTO for Statistic
//new update

package com.graduationproject.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopProductDTO {
    private int productId;
    private String productName;
    private long totalQuantity; // Tổng số lượng bán
    private BigDecimal totalRevenue; // Tổng doanh thu từ sản phẩm này (có thể tính hoặc lấy từ query khác)

    // Constructor cho kết quả từ query theo số lượng bán
    public TopProductDTO(int productId, String productName, Long totalQuantity) {
        this.productId = productId;
        this.productName = productName;
        this.totalQuantity = totalQuantity != null ? totalQuantity : 0;
        this.totalRevenue = null; // Hoặc BigDecimal.ZERO nếu muốn mặc định
    }

    // Constructor cho kết quả từ query theo doanh thu
     public TopProductDTO(int productId, String productName, BigDecimal totalRevenue) {
         this.productId = productId;
         this.productName = productName;
         this.totalRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
         this.totalQuantity = 0; // Hoặc giá trị mặc định khác
     }
}