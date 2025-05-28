package com.graduationproject.backend.controller;

import com.graduationproject.backend.dto.CreateOrderRequestDTO;
import com.graduationproject.backend.dto.OrderDTO;
import com.graduationproject.backend.entity.User; // Import User
import com.graduationproject.backend.entity.enums.PaymentMethod;
import com.graduationproject.backend.exception.BadRequestException;
import com.graduationproject.backend.exception.ResourceNotFoundException;
import com.graduationproject.backend.service.OrderService;
import com.graduationproject.backend.service.UserService; // Import UserService
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException; // Import AccessDeniedException
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;


@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;

    @Autowired
    public OrderController(OrderService orderService, UserService userService) {
        this.orderService = orderService;
        this.userService = userService;
    }

    // --- Helper lấy User ---
    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Kiểm tra authentication có null không và có được xác thực không
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() instanceof String && "anonymousUser".equals(authentication.getPrincipal())) {
            throw new SecurityException("User not authenticated."); // Hoặc AccessDeniedException phù hợp hơn
        }
        // Lấy username từ Principal (UserDetails)
        String username;
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {

            throw new SecurityException("Không thể xác định username từ principal xác thực.");
        }

        return userService.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "username", username + " (từ token)"));
    }

    @PostMapping("/create")
    public ResponseEntity<?> createOrder(
            @Valid @RequestBody CreateOrderRequestDTO orderRequest,
            HttpServletRequest request) {
        // Lấy userId từ người dùng đang đăng nhập
        long userId = getCurrentAuthenticatedUser().getUserId();
        // Lấy IP của client gửi request
        String clientIpAddress = request.getRemoteAddr();

        Map<String, Object> result = orderService.createOrderFromSelectedItems(userId, orderRequest, clientIpAddress);

        // Trả về kết quả cho frontend
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }


    // Endpoint lấy danh sách đơn hàng của người dùng đang đăng nhập
    @GetMapping("/me")
    public ResponseEntity<Page<OrderDTO>> getMyOrders(
            // Sử dụng PageableDefault để cung cấp các giá trị mặc định cho phân trang và sắp xếp
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        // Lấy thông tin người dùng đang đăng nhập
        User currentUser = getCurrentAuthenticatedUser();
        long userId = currentUser.getUserId();

        // Gọi service để tìm các đơn hàng của người dùng đó theo phân trang
        Page<OrderDTO> myOrdersPage = orderService.findOrdersByUserId(userId, pageable);
        return ResponseEntity.ok(myOrdersPage);
    }


    // Endpoint lấy thông tin chi tiết đơn hàng theo ID (có kiểm tra quyền)
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDTO> getOrder(@PathVariable int orderId) {
        // Lấy thông tin người dùng đang đăng nhập
        User currentUser = getCurrentAuthenticatedUser();
        // Gọi service để lấy chi tiết đơn hàng (service sẽ ném 404 nếu không tìm thấy)
        OrderDTO orderDTO = orderService.findOrderDTOById(orderId);

        boolean isAdmin = currentUser.getRole().name().equals("ADMIN");
        boolean isOwner = orderDTO.getUserId() == currentUser.getUserId();

        // Nếu không phải chủ đơn hàng và không phải Admin, ném lỗi AccessDenied
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("Người dùng không có quyền xem đơn hàng này.");
        }

        // Trả về thông tin đơn hàng nếu có quyền
        return ResponseEntity.ok(orderDTO);
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<String> cancelOrder(
            @PathVariable int orderId,
            @RequestBody Map<String, String> payload // Nhận lý do từ request body
    ) {
        // Lấy thông tin người dùng đang đăng nhập
        User currentUser = getCurrentAuthenticatedUser();

        OrderDTO orderToCheck = orderService.findOrderDTOById(orderId);

        // Kiểm tra quyền (là chủ đơn hàng HOẶC là Admin)
        boolean isAdmin = currentUser.getRole().name().equals("ADMIN");
        boolean isOwner = orderToCheck.getUserId() == currentUser.getUserId();

        // Nếu không có quyền, ném lỗi AccessDenied
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("Người dùng không có quyền hủy đơn hàng này.");
        }

        // Lấy lý do từ payload request body
        String reason = payload.get("reason");
        if (reason == null || reason.trim().isEmpty()) {
            // Ném lỗi BadRequest nếu lý do hủy bị thiếu hoặc rỗng
            throw new BadRequestException("Lý do hủy đơn hàng là bắt buộc.");
        }

        orderService.cancelOrder(orderId, reason);

        // Trả về response thành công
        return ResponseEntity.ok("Đơn hàng đã được hủy thành công. Nếu đã thanh toán, quá trình hoàn tiền sẽ được xử lý.");
    }

    @GetMapping("/vnpay_return")
    public ResponseEntity<?> vnpayReturn(@RequestParam Map<String, String> allRequestParams) {

        // Lấy các tham số quan trọng từ phản hồi VNPay
        String vnp_TxnRef = allRequestParams.get("vnp_TxnRef"); // Đây chính là Order ID của bạn
        String vnp_ResponseCode = allRequestParams.get("vnp_ResponseCode"); // Mã phản hồi từ VNPay

        String frontendResultUrl = "http://localhost:5173/payment/result";

        // Thêm các tham số cơ bản vào URL chuyển hướng
        frontendResultUrl += "?status=" + ("00".equals(vnp_ResponseCode) ? "success" : "failed"); // Trạng thái chung
        frontendResultUrl += "&code=" + vnp_ResponseCode; // Mã phản hồi VNPay
        frontendResultUrl += "&orderId=" + (vnp_TxnRef != null ? vnp_TxnRef : "0"); // Order ID (dùng 0 nếu null để tránh lỗi)

        try {

            OrderDTO updatedOrder = orderService.handleVnpayReturn(allRequestParams);

            frontendResultUrl = "http://localhost:5173/payment/result"
                    + "?orderId=" + updatedOrder.getOrderId() // Truyền Order ID thực tế đã được backend xử lý
                    // Có thể tùy chọn truyền thêm trạng thái cuối cùng từ backend
                    + "&status=" + updatedOrder.getStatus().toLowerCase();


            // Trả về response chuyển hướng (HTTP status 302 Found)
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendResultUrl)) // Đặt header Location là URL frontend
                    .build();

        } catch (Exception e) {

            System.err.println("Lỗi xử lý kết quả VNPay cho đơn hàng " + vnp_TxnRef + ": " + e.getMessage());
            e.printStackTrace(); // In stack trace để debug


            frontendResultUrl = "http://localhost:5173/payment/result"
                    + "?orderId=" + (vnp_TxnRef != null ? vnp_TxnRef : "0") // Vẫn cố gắng truyền orderId nếu có
                    + "&status=error" // Báo hiệu lỗi xử lý ở backend
                    + "&code=backend_error"; // Mã lỗi chung cho frontend

            // Trả về response chuyển hướng với trạng thái lỗi
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendResultUrl))
                    .build();
        }
    }


}