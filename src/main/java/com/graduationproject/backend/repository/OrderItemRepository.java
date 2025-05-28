package com.graduationproject.backend.repository;

import com.graduationproject.backend.entity.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {



    //new update

    // Top sản phẩm bán chạy nhất (theo số lượng bán) --Chỉ tính các đơn đã thanh toán/hoàn thành
    @Query("""
           SELECT oi.product.productId as productId, oi.product.name as productName, SUM(oi.quantity) as totalQuantity
           FROM OrderItem oi
           JOIN oi.order o
           WHERE (o.status = 'PAID' OR o.status = 'COMPLETED') 
           GROUP BY oi.product.productId, oi.product.name
           ORDER BY totalQuantity DESC
           """)
    List<Object[]> findTopSellingProducts(Pageable pageable); // Dùng Pageable để giới hạn số lượng (Top N)

    // Có thể thêm query cho top sản phẩm theo doanh thu nếu cần
    @Query("""
            SELECT oi.product.productId as productId, oi.product.name as productName, SUM(oi.price * oi.quantity) as totalRevenue
            FROM OrderItem oi
            JOIN oi.order o
            WHERE (o.status = 'PAID' OR o.status = 'COMPLETED')
            GROUP BY oi.product.productId, oi.product.name
            ORDER BY totalRevenue DESC
            """)
    List<Object[]> findTopRevenueProducts(Pageable pageable);
}
