package com.graduationproject.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Table(name = "invoices")
@Data
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long invoiceId;
    
    // Liên kết đơn hàng
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    // Các thông tin xuất hóa đơn
    @Column(nullable = false, length = 100)
    private String invoiceNumber;  // Mã hóa đơn, có thể tự sinh ra
    
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;
    
    @CreationTimestamp
    private Timestamp createdAt;
}
