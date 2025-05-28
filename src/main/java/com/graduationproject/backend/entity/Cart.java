package com.graduationproject.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
@Data // Bao gồm getter, setter, toString, equals, hashCode
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_id")
    private Long cartId;

    // Có thể dùng OneToOne nếu User chỉ có 1 Cart và muốn truy cập Cart từ User
    // @OneToOne
    // @JoinColumn(name = "user_id", nullable = false, unique = true)
    // Hoặc chỉ lưu userId nếu không cần map ngược từ User
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(name = "last_updated")
    private Timestamp lastUpdated;

    // Quan hệ OneToMany với CartItem
    // EAGER loading có thể tiện lợi cho Cart nhưng cần cân nhắc hiệu năng nếu cart quá lớn
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @ToString.Exclude // Tránh vòng lặp toString
    @EqualsAndHashCode.Exclude // Tránh vòng lặp equals/hashCode
    private List<CartItem> items = new ArrayList<>();


    public void addItem(CartItem item) {
        items.add(item);
        item.setCart(this);
    }

    public void removeItem(CartItem item) {
        items.remove(item);
        item.setCart(null);
    }
}