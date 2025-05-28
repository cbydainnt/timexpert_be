package com.graduationproject.backend.dto;

import com.graduationproject.backend.entity.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequestDTO {

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    // Các trường thông tin giao hàng
    @NotBlank(message = "Full name is required")
    private String fullNameShipping;

    @NotBlank(message = "Phone number is required")
    private String phoneShipping;

    @NotBlank(message = "Address is required")
    private String addressShipping;

    private String notes;

    @NotEmpty(message = "Order must contain at least one item")
    private List<@Valid SelectedItemDTO> items; // Danh sách các item được chọn
}
