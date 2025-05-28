package com.graduationproject.backend.controller;

import com.graduationproject.backend.dto.InvoiceDetailDTO;
import com.graduationproject.backend.entity.Invoice;
import com.graduationproject.backend.entity.Order;
import com.graduationproject.backend.entity.User;
import com.graduationproject.backend.exception.ResourceNotFoundException;
import com.graduationproject.backend.service.InvoiceService;
import com.graduationproject.backend.service.OrderService;
import com.graduationproject.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException; // Import AccessDeniedException
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoices") // Base path /api/invoices
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final UserService userService;
    private final OrderService orderService;
    @Autowired
    public InvoiceController(InvoiceService invoiceService, UserService userService, OrderService orderService) {
        this.invoiceService = invoiceService;
        this.userService = userService;
        this.orderService = orderService;
    }

    // --- Helper ---
    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new SecurityException("User not authenticated.");
        }
        String username = ((UserDetails)authentication.getPrincipal()).getUsername();
        return userService.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username + " from token"));
    }
    // --- End Helper ---

    /**
     * Lấy hoặc tạo hóa đơn cho một đơn hàng cụ thể.
     * Chỉ chủ đơn hàng hoặc Admin mới có quyền truy cập.
     */
//    @GetMapping("/order/{orderId}") // Dùng path này để lấy/tạo hóa đơn theo orderId
//    public ResponseEntity<Invoice> getOrCreateInvoiceForOrder(@PathVariable int orderId) {
//        User currentUser = getCurrentAuthenticatedUser();
//        // Gọi service để lấy hoặc tạo hóa đơn
//        Invoice invoice = invoiceService.generateOrGetInvoiceForOrder(orderId); // Gọi phương thức mới
//
//        // Kiểm tra quyền truy cập (sau khi đã có invoice và thông tin order)
//        if (invoice.getOrder() == null) {
//            // Trường hợp hiếm gặp: Service trả về invoice mà không có order?
//            throw new ResourceNotFoundException("Order information missing in the invoice.");
//        }
//
//        boolean isAdmin = currentUser.getRole().name().equals("ADMIN");
//        boolean isOwner = invoice.getOrder().getUserId() == currentUser.getUserId();
//
//        if (!isOwner && !isAdmin) {
//            // Ném lỗi rõ ràng hơn thay vì SecurityException chung chung
//            throw new AccessDeniedException("You do not have permission to access the invoice for order " + orderId);
//            // Hoặc return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
//        }
//
//        return ResponseEntity.ok(invoice);
//    }
    @GetMapping("/order/{orderId}")
    // 1. Thay đổi kiểu trả về thành ResponseEntity<InvoiceDetailDTO>
    public ResponseEntity<InvoiceDetailDTO> getOrCreateInvoiceForOrder(@PathVariable int orderId) {
        User currentUser = getCurrentAuthenticatedUser();

        // 2. Gọi phương thức service mới trả về InvoiceDetailDTO
        InvoiceDetailDTO invoiceDetailDTO = invoiceService.getInvoiceDetails(orderId); // Giả sử phương thức này đã có trong InvoiceService

        // 3. Kiểm tra quyền truy cập
        // Lấy Order entity gốc để kiểm tra userId một cách đáng tin cậy
        Order orderEntity = orderService.findOrderEntityById(orderId);
        if (orderEntity == null) { // Service nên ném lỗi nếu order không tồn tại trước khi đến đây
            throw new ResourceNotFoundException("Order", "orderId", orderId);
        }

        boolean isAdmin = currentUser.getRole().name().equals("ADMIN");
        boolean isOwner = orderEntity.getUserId() == currentUser.getUserId();

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You do not have permission to access the invoice for order " + orderId);
        }

        return ResponseEntity.ok(invoiceDetailDTO);
    }

    /*
    // Endpoint này có thể không cần thiết nữa nếu GET /order/{orderId} đã xử lý cả việc lấy hóa đơn cũ
    // Nếu vẫn muốn giữ, path nên là /order/{orderId}/existing hoặc tương tự

    @GetMapping("/order/{orderId}/existing") // Ví dụ path rõ ràng hơn
     public ResponseEntity<Invoice> getExistingInvoiceByOrderId(@PathVariable int orderId) {
          User currentUser = getCurrentAuthenticatedUser();
          Invoice invoice = invoiceService.getExistingInvoiceByOrderId(orderId); // Gọi service chỉ lấy hóa đơn cũ

          if (invoice == null) {
               return ResponseEntity.notFound().build();
          }

          // Kiểm tra quyền
          boolean isAdmin = currentUser.getRole().name().equals("ADMIN");
          boolean isOwner = invoice.getOrder() != null && invoice.getOrder().getUserId() == currentUser.getUserId();

           if (!isOwner && !isAdmin) {
               throw new AccessDeniedException("You do not have permission to access the invoice for order " + orderId);
           }

          return ResponseEntity.ok(invoice);
     }
     */

}