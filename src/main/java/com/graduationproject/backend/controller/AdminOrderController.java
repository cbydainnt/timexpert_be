package com.graduationproject.backend.controller;

import com.graduationproject.backend.dto.OrderDTO;
import com.graduationproject.backend.entity.enums.OrderStatus;
import com.graduationproject.backend.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page; // Import Page
import org.springframework.data.domain.PageRequest; // Import PageRequest
import org.springframework.data.domain.Pageable; // Import Pageable
import org.springframework.data.domain.Sort; // Import Sort
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // Import PreAuthorize
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders") // Base path cho quản lý đơn hàng của admin
//@PreAuthorize("hasRole('ADMIN')") // Yêu cầu quyền ADMIN cho tất cả API
public class AdminOrderController {

    private final OrderService orderService;

    @Autowired
    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * API Lấy danh sách tất cả đơn hàng cho Admin (có phân trang, sắp xếp, lọc).
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Page<OrderDTO>> getAllOrders(
            @RequestParam(required = false) OrderStatus status, // Lọc theo trạng thái (ví dụ: ?status=PENDING)
            @RequestParam(required = false) Long userId,       // Lọc theo user ID (ví dụ: ?userId=123)
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy, // Sắp xếp theo cột nào
            @RequestParam(defaultValue = "desc") String sortDir    // asc hoặc desc
    ) {
        // Validate sortDir
        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        // Validate sortBy field (nên giới hạn các cột được phép sort)
        String sortField = "createdAt"; // Mặc định
        if (sortBy.equalsIgnoreCase("orderId") || sortBy.equalsIgnoreCase("totalAmount") || sortBy.equalsIgnoreCase("status")) {
             sortField = sortBy;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        // Gọi service mới để lấy danh sách order có lọc
        Page<OrderDTO> orderPage = orderService.findAllOrdersForAdmin(status, userId, pageable);
        return ResponseEntity.ok(orderPage);
    }

    /**
     * API Xem chi tiết một đơn hàng cụ thể (Admin).
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<OrderDTO> getOrderDetailsForAdmin(@PathVariable int orderId) {
        // Admin có quyền xem mọi đơn hàng, dùng lại phương thức cũ của OrderService
        OrderDTO orderDTO = orderService.findOrderDTOById(orderId);
        return ResponseEntity.ok(orderDTO);
    }

    /**
     * API Cập nhật trạng thái của một đơn hàng.
     */
    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<OrderDTO> updateOrderStatus(
            @PathVariable int orderId,
            @RequestParam OrderStatus newStatus // Nhận trạng thái mới từ query param (ví dụ: ?newStatus=PROCESSING)
            // Hoặc dùng @RequestBody nếu muốn gửi JSON: @RequestBody Map<String, String> body -> OrderStatus.valueOf(body.get("status"))
    ) {
        // Service sẽ xử lý logic kiểm tra chuyển trạng thái hợp lệ và ném lỗi nếu không được
        OrderDTO updatedOrderDTO = orderService.updateOrderStatus(orderId, newStatus);
        return ResponseEntity.ok(updatedOrderDTO);
    }


    // Có thể thêm các API khác cho Admin nếu cần
    // Ví dụ: Tìm kiếm đơn hàng theo mã giao dịch VNPay, theo sản phẩm,...
}