package com.graduationproject.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductReviewDTO {
    private Long reviewId;
    private int productId;
    private Long userId;
    private String productName;
    private String userFirstName;
    private String userLastName;
    private int rating;
    private String comment;
    private Timestamp reviewDate;
    private boolean visible;
}