package com.graduationproject.backend.dto;

import com.graduationproject.backend.entity.enums.PaymentMethod; // Thêm import
import lombok.Data;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

@Data
public class OrderDTOForInvoice {
    private int orderId;
    private Timestamp orderCreatedAt; // Ngày đặt hàng
    private String customerFullName; // Lấy từ fullNameShipping của Order entity
    private String customerAddress;  // Lấy từ addressShipping của Order entity
    private String customerPhone;    // Lấy từ phoneShipping của Order entity
    private String customerEmail;    // Sẽ lấy từ User entity liên kết với Order
    private List<OrderItemDTOForInvoice> orderItems;
    private BigDecimal totalAmount;  // Tổng tiền cuối cùng của đơn hàng
    private String paymentMethod;    // Dạng String của PaymentMethod Enum
    private String notes;            // Ghi chú đơn hàng
    // Các trường cần thiết khác như tổng tiền hàng (chưa có phí, giảm giá), phí ship, giảm giá
    private BigDecimal subTotal; // Tổng tiền hàng (sum of item.price * item.quantity)
    private BigDecimal shippingFee; // Phí vận chuyển (nếu có)
    private BigDecimal discountAmount; // Số tiền giảm giá (nếu có)
    private long userId;
}