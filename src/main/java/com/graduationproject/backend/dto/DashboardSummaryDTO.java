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
public class DashboardSummaryDTO {
    private long totalOrders; // Tổng số đơn hàng
    private long pendingOrders; // Đơn hàng đang chờ
    private long completedOrders; // Đơn hàng hoàn thành
    private long totalCustomers; // Tổng số người dùng (BUYER)
    private long totalProducts; // Tổng số sản phẩm
    private BigDecimal totalRevenue; // Tổng doanh thu từ trước đến nay hoặc trong tháng/năm (tùy định nghĩa)
    // Có thể thêm các trường khác như newCustomersInPeriod, newOrdersInPeriod...
}