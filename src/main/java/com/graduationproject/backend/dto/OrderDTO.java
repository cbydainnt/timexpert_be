package com.graduationproject.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

@Data
public class OrderDTO {
    private int orderId;
    private long userId;
    private BigDecimal totalAmount;
    private String status;
    private String paymentMethod;
    private String vnpayTransactionId; // Giữ lại để tham khảo
    private Timestamp createdAt;

    private String fullNameShipping;
    private String phoneShipping;
    private String addressShipping;
    private String notes;
    private List<OrderItemDTO> orderItems; // Sử dụng OrderItemDTO đã cập nhật

}