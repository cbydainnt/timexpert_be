package com.graduationproject.backend.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductRequestDTO {
    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;
    private String description;

    @NotNull(message = "Giá không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá phải lớn hơn 0")
    private BigDecimal price;

    @NotNull(message = "Số lượng tồn kho không được để trống")
    @Min(value = 0, message = "Số lượng tồn kho không được âm")
    private Integer stock; // Sử dụng Integer để cho phép null nếu logic của bạn cần

    @NotNull(message = "Danh mục không được để trống")
    private Integer categoryId;

    private String barcode;
    private String brand;
    private String model;
    private String movement;
    private String caseMaterial;
    private String strapMaterial;
    private String dialColor;
    private String waterResistance;

    private List<String> imageUrls; // Danh sách URL ảnh đã upload
    private String primaryImageUrl; // URL của ảnh chính (phải có trong imageUrls hoặc là một trong số chúng)
}