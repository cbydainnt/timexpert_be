package com.graduationproject.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page; // Import Page

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductPageDTO {
    private List<ProductDTO> content;        // Danh sách sản phẩm trên trang hiện tại
    private int currentPage;                 // Số trang hiện tại (bắt đầu từ 0)
    private int totalPages;                  // Tổng số trang
    private long totalItems;                 // Tổng số sản phẩm
    private int pageSize;                    // Kích thước trang
    private boolean isFirst;                 // Trang đầu tiên?
    private boolean isLast;                  // Trang cuối cùng?

    // Constructor để dễ dàng tạo từ đối tượng Page của Spring Data
    public static ProductPageDTO fromPage(Page<ProductDTO> page) {
        return new ProductPageDTO(
                page.getContent(),
                page.getNumber(),      // Số trang hiện tại (từ 0)
                page.getTotalPages(),
                page.getTotalElements(),
                page.getSize(),
                page.isFirst(),
                page.isLast()
        );
    }
}