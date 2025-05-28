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
public class CustomerStatisticDTO {
    private long userId;
    private String customerName;
    private long orderCount; // Số đơn hàng hoàn thành hoặc bị hủy
    private BigDecimal totalRevenue; // Tổng doanh thu từ khách hàng này (nếu thống kê theo doanh thu)

     // Constructor cho kết quả từ query theo số lượng đơn (hoàn thành/hủy)
     public CustomerStatisticDTO(Long userId, String customerName, Long orderCount) {
         this.userId = userId != null ? userId : 0;
         this.customerName = customerName;
         this.orderCount = orderCount != null ? orderCount : 0;
         this.totalRevenue = null; // Hoặc BigDecimal.ZERO
     }

     // Constructor cho kết quả từ query theo doanh thu khách hàng
      public CustomerStatisticDTO(Long userId, String customerName, BigDecimal totalRevenue) {
          this.userId = userId != null ? userId : 0;
          this.customerName = customerName;
          this.totalRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
          this.orderCount = 0; // Hoặc giá trị mặc định khác
      }
}