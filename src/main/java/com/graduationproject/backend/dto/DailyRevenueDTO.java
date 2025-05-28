//DTO for Statistic
//new update

package com.graduationproject.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date; // Sử dụng java.util.Date hoặc java.time.LocalDate tùy bạn

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyRevenueDTO {
    private Date date; // Ngày
    private BigDecimal revenue; // Doanh thu ngày đó
}