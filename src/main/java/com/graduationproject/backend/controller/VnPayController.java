package com.graduationproject.backend.controller;

import com.graduationproject.backend.entity.Order;
import com.graduationproject.backend.entity.enums.OrderStatus;
import com.graduationproject.backend.repository.OrderRepository;
import com.graduationproject.backend.service.VnPayService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/vnpay")
@RequiredArgsConstructor
public class VnPayController {

    private final VnPayService vnPayService;
    private final OrderRepository orderRepository;

    @GetMapping("/return")
    public String handleVnPayReturn(@RequestParam Map<String, String> allParams) {
        // Bước 1: Xác thực chữ ký trả về
        boolean isValid = vnPayService.validateReturnSignature(allParams);
        if (!isValid) {
            return "Chữ ký không hợp lệ. Giao dịch bị từ chối.";
        }

        // Bước 2: Lấy thông tin đơn hàng từ vnp_TxnRef
        int orderId = Integer.parseInt(allParams.get("vnp_TxnRef"));
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return "Không tìm thấy đơn hàng.";
        }

        // Bước 3: Kiểm tra mã phản hồi
        String responseCode = allParams.get("vnp_ResponseCode");
        if ("00".equals(responseCode)) {
            // Thành công
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);
            return "Giao dịch thành công. Cảm ơn bạn đã thanh toán!";
        } else {
            // Thất bại
            order.setStatus(OrderStatus.CANCELED);
            orderRepository.save(order);
            return "Giao dịch thất bại. Mã lỗi: " + responseCode;
        }
    }
}
