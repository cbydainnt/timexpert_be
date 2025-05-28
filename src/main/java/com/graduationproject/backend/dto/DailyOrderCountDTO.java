//DTO for Statistic
//new update

package com.graduationproject.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date; // Sử dụng java.util.Date hoặc java.time.LocalDate tùy bạn

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyOrderCountDTO {
    private Date date; // Ngày
    private long orderCount; // Số lượng đơn hàng hoàn thành/đã thanh toán ngày đó
}