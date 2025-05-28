package com.graduationproject.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

@Data
public class InvoiceDetailDTO {
    private String invoiceNumber;
    private Timestamp createdAt; // Ngày xuất hóa đơn
    private OrderDTOForInvoice order; // DTO con cho Order
    // Thông tin cửa hàng/công ty (có thể hardcode hoặc lấy từ config)
    private String storeName = "TimeXpert Store";
    private String storeAddress = "39 Phố Thi Sách, P. Phạm Đình Hổ, Q. Hai Bà Trưng, Hà Nội";
    private String storePhone = "028 3456 8899";
    private String storeEmail = "support@timexpert.vn";
    private String storeTaxCode = "0312345678";
}