package com.graduationproject.backend.dto;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class FavoriteDTO {
    private long id; // ID của bản ghi favorite
    private long userId;
    private ProductDTO product; // Trả về thông tin chi tiết sản phẩm
    private Timestamp createdAt;
}