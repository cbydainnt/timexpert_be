//package com.graduationproject.backend.controller;
//
//import com.graduationproject.backend.dto.OrderDTO;
//import com.graduationproject.backend.entity.Order;
//import com.graduationproject.backend.service.OrderService;
//import com.graduationproject.backend.service.VnPayService;
//import jakarta.servlet.http.HttpServletRequest;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.net.URI;
//import java.util.HashMap;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/payment")
//public class PaymentController {
//
//    private final VnPayService vnPayService;
//    private final OrderService orderService; // Giả sử bạn có service để tạo và cập nhật đơn hàng
//
//    @Autowired
//    public PaymentController(VnPayService vnPayService, OrderService orderService) {
//        this.vnPayService = vnPayService;
//        this.orderService = orderService;
//    }
//
//    @PostMapping("/vnpay")
//    public ResponseEntity<String> createPaymentUrl(@RequestBody OrderDTO orderDTO, HttpServletRequest request) {
//        String ipAddress = request.getRemoteAddr();
//        Order order = orderService.createPendingOrder(orderDTO); // trạng thái PENDING
//        String paymentUrl = vnPayService.createPaymentUrl(order, ipAddress);
//        return ResponseEntity.ok(paymentUrl);
//    }
//
//    @GetMapping("/vnpay/return")
//    public ResponseEntity<String> handleVnPayReturn(@RequestParam Map<String, String> params) {
//        boolean isValid = vnPayService.validateReturnSignature(new HashMap<>(params));
//        String responseCode = params.get("vnp_ResponseCode");
//        String orderId = params.get("vnp_TxnRef");
//
//        if (!isValid) {
//            return ResponseEntity.badRequest().body("Invalid signature");
//        }
//
//        if ("00".equals(responseCode)) {
//            orderService.markOrderAsPaid(Long.parseLong(orderId));
//            return ResponseEntity.status(HttpStatus.FOUND)
//                .location(URI.create("http://localhost:5173/payment_return?success=true"))
//                .build();
//        } else {
//            orderService.cancelOrder(Long.parseLong(orderId));
//            return ResponseEntity.status(HttpStatus.FOUND)
//                .location(URI.create("http://localhost:5173/payment_return?success=false"))
//                .build();
//        }
//    }
//}
