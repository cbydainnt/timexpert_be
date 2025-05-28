package com.graduationproject.backend.repository;

import com.graduationproject.backend.entity.Order;
import com.graduationproject.backend.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Integer>, JpaSpecificationExecutor<Order> {
    Page<Order> findByUserId(long userId, Pageable pageable);


    //new update

    // Thống kê tổng số đơn hàng theo trạng thái trong một khoảng thời gian
    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate AND o.status = :status")
    long countByCreatedAtBetweenAndStatus(@Param("startDate") Timestamp startDate, @Param("endDate") Timestamp endDate, @Param("status") OrderStatus status);

    // Thống kê tổng doanh thu (từ các đơn hàng PAID hoặc COMPLETED) trong một khoảng thời gian
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate AND (o.status = 'PAID' OR o.status = 'COMPLETED')")
    BigDecimal sumTotalAmountByCreatedAtBetweenAndStatusIn(@Param("startDate") Timestamp startDate, @Param("endDate") Timestamp endDate);

    // Thống kê số lượng đơn hàng mỗi ngày trong một khoảng thời gian (chỉ các đơn hoàn thành hoặc đã thanh toán)
    @Query("SELECT DATE(o.createdAt) as orderDate, COUNT(o) as orderCount FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate AND (o.status = 'PAID' OR o.status = 'COMPLETED') GROUP BY DATE(o.createdAt) ORDER BY orderDate")
    List<Object[]> countCompletedOrdersByDay(@Param("startDate") Timestamp startDate, @Param("endDate") Timestamp endDate);

    // Thống kê doanh thu mỗi ngày trong một khoảng thời gian (chỉ các đơn hoàn thành hoặc đã thanh toán)
    @Query("SELECT DATE(o.createdAt) as orderDate, SUM(o.totalAmount) as totalRevenue FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate AND (o.status = 'PAID' OR o.status = 'COMPLETED') GROUP BY DATE(o.createdAt) ORDER BY orderDate")
    List<Object[]> sumRevenueByDay(@Param("startDate") Timestamp startDate, @Param("endDate") Timestamp endDate);

    // Thống kê doanh thu theo danh mục trong một khoảng thời gian (chỉ các đơn hoàn thành hoặc đã thanh toán)
    @Query("""
           SELECT c.name as categoryName, SUM(oi.price * oi.quantity) as categoryRevenue
           FROM Order o
           JOIN o.orderItems oi
           JOIN oi.product p
           JOIN p.category c
           WHERE o.createdAt BETWEEN :startDate AND :endDate
             AND (o.status = 'PAID' OR o.status = 'COMPLETED')
           GROUP BY c.name
           ORDER BY categoryRevenue DESC
           """)
    List<Object[]> sumRevenueByCategory(@Param("startDate") Timestamp startDate, @Param("endDate") Timestamp endDate);

    // Thống kê doanh thu theo khách hàng trong một khoảng thời gian (chỉ các đơn hoàn thành hoặc đã thanh toán)
    // Cần fetch tên khách hàng từ bảng User - cần join User
    @Query("""
           SELECT u.userId as userId, CONCAT(u.firstName, ' ', u.lastName) as customerName, SUM(o.totalAmount) as customerRevenue
           FROM Order o
           JOIN User u ON o.userId = u.userId
           WHERE o.createdAt BETWEEN :startDate AND :endDate
             AND (o.status = 'PAID' OR o.status = 'COMPLETED')
           GROUP BY u.userId, customerName
           ORDER BY customerRevenue DESC
           """)
    List<Object[]> sumRevenueByCustomer(@Param("startDate") Timestamp startDate, @Param("endDate") Timestamp endDate);

    // Top khách hàng theo số lượng đơn HOÀN THÀNH
    @Query("""
            SELECT u.userId as userId, CONCAT(u.firstName, ' ', u.lastName) as customerName, COUNT(o) as orderCount
            FROM Order o
            JOIN User u ON o.userId = u.userId
            WHERE o.status = 'COMPLETED'
            GROUP BY u.userId, customerName
            ORDER BY orderCount DESC
            """)
    List<Object[]> countCompletedOrdersByCustomer(Pageable pageable); // Dùng Pageable để giới hạn số lượng (Top N)

    // Top khách hàng theo số lượng đơn BỊ HỦY
    @Query("""
             SELECT u.userId as userId, CONCAT(u.firstName, ' ', u.lastName) as customerName, COUNT(o) as cancelCount
             FROM Order o
             JOIN User u ON o.userId = u.userId
             WHERE o.status = 'CANCELED'
             GROUP BY u.userId, customerName
             ORDER BY cancelCount DESC
             """)
    List<Object[]> countCanceledOrdersByCustomer(Pageable pageable); // Dùng Pageable để giới hạn số lượng (Top N)

    // Tổng số đơn hàng (cho dashboard)
    long count();

    // Tổng số đơn hàng theo trạng thái (cho dashboard)
    long countByStatus(OrderStatus status);
}
