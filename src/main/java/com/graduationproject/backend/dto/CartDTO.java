package com.graduationproject.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartDTO {
    private long userId;
    private List<CartItemDetailDTO> items; // <<< Danh sách item chi tiết
    private Date lastUpdated;
}