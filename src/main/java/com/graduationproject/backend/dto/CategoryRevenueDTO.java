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
public class CategoryRevenueDTO {
    private String categoryName;
    private BigDecimal totalRevenue;
}