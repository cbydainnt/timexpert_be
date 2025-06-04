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
    private String vnpayTransactionId;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    private String fullNameShipping;
    private String phoneShipping;
    private String addressShipping;
    private String notes;
    private List<OrderItemDTO> orderItems;

}