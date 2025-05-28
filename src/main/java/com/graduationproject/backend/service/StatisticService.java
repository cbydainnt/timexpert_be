package com.graduationproject.backend.service;

import com.graduationproject.backend.dto.*; // Import tất cả DTO thống kê mới
import com.graduationproject.backend.entity.enums.OrderStatus; // Import OrderStatus
import com.graduationproject.backend.entity.enums.Role;
import com.graduationproject.backend.repository.OrderItemRepository; // Inject OrderItemRepository
import com.graduationproject.backend.repository.OrderRepository; // Inject OrderRepository
import com.graduationproject.backend.repository.ProductRepository; // Inject ProductRepository
import com.graduationproject.backend.repository.UserRepository; // Inject UserRepository
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import Transactional

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap; // Dùng LinkedHashMap để giữ thứ tự
import java.util.stream.Collectors;

@Service
public class StatisticService {

    // Inject các Repository cần thiết
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    // Hàm helper chuyển đổi String ngày (YYYY-MM-DD) sang Timestamp (bắt đầu ngày)
    private Timestamp getStartOfDayTimestamp(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            LocalDate date = LocalDate.parse(dateStr);
            LocalDateTime startOfDay = date.atStartOfDay();
            return Timestamp.valueOf(startOfDay);
        } catch (Exception e) {
            // Xử lý lỗi parse ngày nếu cần
            return null;
        }
    }

    // Hàm helper chuyển đổi String ngày (YYYY-MM-DD) sang Timestamp (cuối ngày)
    private Timestamp getEndOfDayTimestamp(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            LocalDate date = LocalDate.parse(dateStr);
            LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
            return Timestamp.valueOf(endOfDay);
        } catch (Exception e) {
            // Xử lý lỗi parse ngày nếu cần
            return null;
        }
    }


    /**
     * [ADMIN] Lấy dữ liệu tóm tắt nhanh cho Dashboard.
     * Số liệu tổng quan (tổng đơn, user, sản phẩm, doanh thu...).
     * @return DashboardSummaryDTO
     */
    @Transactional(readOnly = true)
    public DashboardSummaryDTO getDashboardSummary() {
        // Lấy các số liệu tổng từ các Repository
        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);
        long completedOrders = orderRepository.countByStatus(OrderStatus.COMPLETED);
        long totalCustomers = userRepository.countByRole(Role.BUYER); // Chỉ đếm role BUYER
        long totalProducts = productRepository.count();

        // Tính tổng doanh thu toàn bộ (từ các đơn PAID/COMPLETED)
        // Bạn có thể giới hạn trong 1 tháng/năm nếu muốn, hoặc lấy tổng từ trước đến nay
        // Ví dụ lấy tổng từ trước đến nay, cần khoảng thời gian rất rộng
        LocalDateTime startOfTime = LocalDateTime.of(2000, 1, 1, 0, 0); // Giả định bắt đầu từ năm 2000
        LocalDateTime endOfTime = LocalDateTime.now().plusYears(1); // Giả định đến 1 năm sau
        Timestamp startDate = Timestamp.valueOf(startOfTime);
        Timestamp endDate = Timestamp.valueOf(endOfTime);
        BigDecimal totalRevenue = orderRepository.sumTotalAmountByCreatedAtBetweenAndStatusIn(startDate, endDate);

        // TODO: Có thể thêm tính số liệu "mới trong tháng/tuần" nếu cần chi tiết hơn cho dashboard

        return new DashboardSummaryDTO(
                totalOrders,
                pendingOrders,
                completedOrders,
                totalCustomers,
                totalProducts,
                totalRevenue != null ? totalRevenue : BigDecimal.ZERO
        );
    }


    /**
     * [ADMIN] Lấy thống kê doanh thu theo ngày trong một khoảng thời gian.
     * @param fromDateStr Ngày bắt đầu (YYYY-MM-DD)
     * @param toDateStr   Ngày kết thúc (YYYY-MM-DD)
     * @return List<DailyRevenueDTO>
     */
    @Transactional(readOnly = true)
    public List<DailyRevenueDTO> getDailyRevenueStatistics(String fromDateStr, String toDateStr) {
        Timestamp startDate = getStartOfDayTimestamp(fromDateStr);
        Timestamp endDate = getEndOfDayTimestamp(toDateStr);

        if (startDate == null || endDate == null) {
            // Xử lý lỗi ngày không hợp lệ
            throw new IllegalArgumentException("Invalid date format for revenue statistics.");
        }

        List<Object[]> results = orderRepository.sumRevenueByDay(startDate, endDate);

        // Chuyển đổi kết quả từ Object[] sang List<DailyRevenueDTO>
        return results.stream()
                .map(row -> {
                    // row[0] là Date, row[1] là BigDecimal
                    Date date = (Date) row[0];
                    BigDecimal revenue = (BigDecimal) row[1];
                    return new DailyRevenueDTO(date, revenue != null ? revenue : BigDecimal.ZERO);
                })
                .collect(Collectors.toList());
    }

    /**
     * [ADMIN] Lấy thống kê số lượng đơn hàng theo ngày trong một khoảng thời gian.
     * @param fromDateStr Ngày bắt đầu (YYYY-MM-DD)
     * @param toDateStr   Ngày kết thúc (YYYY-MM-DD)
     * @return List<DailyOrderCountDTO>
     */
    @Transactional(readOnly = true)
    public List<DailyOrderCountDTO> getDailyOrderCountStatistics(String fromDateStr, String toDateStr) {
        Timestamp startDate = getStartOfDayTimestamp(fromDateStr);
        Timestamp endDate = getEndOfDayTimestamp(toDateStr);

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Invalid date format for order count statistics.");
        }

        List<Object[]> results = orderRepository.countCompletedOrdersByDay(startDate, endDate);

        // Chuyển đổi kết quả từ Object[] sang List<DailyOrderCountDTO>
        return results.stream()
                .map(row -> {
                    // row[0] là Date, row[1] là Long
                    Date date = (Date) row[0];
                    Long count = (Long) row[1];
                    return new DailyOrderCountDTO(date, count != null ? count : 0);
                })
                .collect(Collectors.toList());
    }


    /**
     * [ADMIN] Lấy thống kê doanh thu theo danh mục trong một khoảng thời gian.
     * @param fromDateStr Ngày bắt đầu (YYYY-MM-DD)
     * @param toDateStr   Ngày kết thúc (YYYY-MM-DD)
     * @return List<CategoryRevenueDTO>
     */
    @Transactional(readOnly = true)
    public List<CategoryRevenueDTO> getRevenueStatisticsByCategory(String fromDateStr, String toDateStr) {
        Timestamp startDate = getStartOfDayTimestamp(fromDateStr);
        Timestamp endDate = getEndOfDayTimestamp(toDateStr);

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Invalid date format for category revenue statistics.");
        }

        List<Object[]> results = orderRepository.sumRevenueByCategory(startDate, endDate);

        // Chuyển đổi kết quả từ Object[] sang List<CategoryRevenueDTO>
        return results.stream()
                .map(row -> {
                    // row[0] là String (categoryName), row[1] là BigDecimal
                    String categoryName = (String) row[0];
                    BigDecimal revenue = (BigDecimal) row[1];
                    return new CategoryRevenueDTO(categoryName, revenue != null ? revenue : BigDecimal.ZERO);
                })
                .collect(Collectors.toList());
    }

    /**
     * [ADMIN] Lấy thống kê doanh thu theo khách hàng trong một khoảng thời gian.
     * @param fromDateStr Ngày bắt đầu (YYYY-MM-DD)
     * @param toDateStr   Ngày kết thúc (YYYY-MM-DD)
     * @param limit Giới hạn số lượng khách hàng (ví dụ: top 10)
     * @return List<CustomerRevenueDTO>
     */
    @Transactional(readOnly = true)
    public List<CustomerRevenueDTO> getRevenueStatisticsByCustomer(String fromDateStr, String toDateStr, int limit) {
         Timestamp startDate = getStartOfDayTimestamp(fromDateStr);
         Timestamp endDate = getEndOfDayTimestamp(toDateStr);

         if (startDate == null || endDate == null) {
             throw new IllegalArgumentException("Invalid date format for customer revenue statistics.");
         }
         if (limit <= 0) limit = 10; // Mặc định top 10

         // Query sumRevenueByCustomer không có limit, cần lấy tất cả rồi giới hạn sau
         // Hoặc viết query native có LIMIT nếu cần tối ưu
         List<Object[]> results = orderRepository.sumRevenueByCustomer(startDate, endDate);

         // Chuyển đổi kết quả và giới hạn số lượng
         return results.stream()
                 .map(row -> new CustomerRevenueDTO(
                         (Long) row[0], // userId
                         (String) row[1], // customerName
                         (BigDecimal) row[2] // customerRevenue
                 ))
                 .limit(limit) // Giới hạn số lượng
                 .collect(Collectors.toList());
    }


    /**
     * [ADMIN] Lấy Top N sản phẩm bán chạy nhất (theo số lượng).
     * @param limit Giới hạn số lượng sản phẩm (ví dụ: top 5)
     * @return List<TopProductDTO>
     */
    @Transactional(readOnly = true)
    public List<TopProductDTO> getTopSellingProducts(int limit) {
        if (limit <= 0) limit = 5; // Mặc định top 5
        Pageable pageable = PageRequest.of(0, limit); // Lấy từ trang 0 với limit

        List<Object[]> results = orderItemRepository.findTopSellingProducts(pageable);

        // Chuyển đổi kết quả từ Object[] sang List<TopProductDTO>
        return results.stream()
                .map(row -> new TopProductDTO(
                        (int) row[0], // productId (cast từ Integer)
                        (String) row[1], // productName
                        (Long) row[2] // totalQuantity
                ))
                .collect(Collectors.toList());
    }

     /**
      * [ADMIN] Lấy Top N khách hàng có số lượng đơn hoàn thành nhiều nhất.
      * @param limit Giới hạn số lượng khách hàng
      * @return List<CustomerStatisticDTO>
      */
     @Transactional(readOnly = true)
     public List<CustomerStatisticDTO> getTopCustomersByCompletedOrders(int limit) {
         if (limit <= 0) limit = 5; // Mặc định top 5
         Pageable pageable = PageRequest.of(0, limit);

         List<Object[]> results = orderRepository.countCompletedOrdersByCustomer(pageable);

         return results.stream()
                 .map(row -> new CustomerStatisticDTO(
                         (Long) row[0], // userId
                         (String) row[1], // customerName
                         (Long) row[2] // orderCount
                 ))
                 .collect(Collectors.toList());
     }

     /**
      * [ADMIN] Lấy Top N khách hàng có số lượng đơn BỊ HỦY nhiều nhất.
      * @param limit Giới hạn số lượng khách hàng
      * @return List<CustomerStatisticDTO>
      */
     @Transactional(readOnly = true)
     public List<CustomerStatisticDTO> getTopCustomersByCanceledOrders(int limit) {
         if (limit <= 0) limit = 5; // Mặc định top 5
         Pageable pageable = PageRequest.of(0, limit);

         List<Object[]> results = orderRepository.countCanceledOrdersByCustomer(pageable);

         return results.stream()
                 .map(row -> new CustomerStatisticDTO(
                         (Long) row[0], // userId
                         (String) row[1], // customerName
                         (Long) row[2] // cancelCount
                 ))
                 .collect(Collectors.toList());
     }


    /**
     * [ADMIN] Lấy thống kê tồn kho (từ logic cũ, giữ nguyên).
     * Trong thực tế cần truy vấn từ bảng Product.
     */
    @Transactional(readOnly = true)
    public Map<String, Integer> getInventoryStatistics() {
        // TODO: Implement real logic using ProductRepository
        // Ví dụ: countByProductStock() hoặc findByStockLessThan()
        // Tạm thời vẫn trả về dữ liệu giả hoặc danh sách sản phẩm với stock
         List<Object[]> inventoryData = productRepository.getInventorySummary(); // Cần thêm query này vào ProductRepository
         Map<String, Integer> inventoryMap = new LinkedHashMap<>(); // Dùng LinkedHashMap để giữ thứ tự nếu query có ORDER BY
         if (inventoryData != null) {
              for (Object[] row : inventoryData) {
                   String productName = (String) row[0];
                  Integer stock = (Integer) row[1]; // Lấy về dưới dạng Integer
                   inventoryMap.put(productName, stock != null ? stock.intValue() : 0);
              }
         }
         return inventoryMap;
    }
}