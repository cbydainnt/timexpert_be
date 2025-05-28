package com.graduationproject.backend.entity;

import com.graduationproject.backend.entity.enums.OrderStatus;
import com.graduationproject.backend.entity.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int orderId;
    
    @Column(nullable = false, length = 50)
    private long userId;
    
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;  // PENDING, PAID, SHIPPED, COMPLETED, CANCELED
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;  // VN_PAY, QR_CODE
    
    @Column(length = 100)
    private String vnpayTransactionId;
    
    @CreationTimestamp
    private Timestamp createdAt;

    @Column
    private String cancellationReason;

    @Column(length = 50)
    private String fullNameShipping;

    @Column(length = 255)
    private String addressShipping;

    @Column(length = 15)
    private String phoneShipping;

    @Column(columnDefinition = "TEXT")
    private  String notes;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();
}
