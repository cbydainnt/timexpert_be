package com.graduationproject.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewRequestDTO {

    @NotNull(message = "Product ID là bắt buộc")
    private Integer productId;

    private Integer orderId;

    @NotNull(message = "Rating là bắt buộc")
    @Min(value = 1, message = "Rating phải từ 1 đến 5")
    @Max(value = 5, message = "Rating phải từ 1 đến 5")
    private Integer rating;

    @Size(max = 1000, message = "Bình luận không được vượt quá 1000 ký tự")
    private String comment;
}