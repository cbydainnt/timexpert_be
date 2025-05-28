package com.graduationproject.backend.service;

import com.graduationproject.backend.dto.CreateOrderRequestDTO;
import com.graduationproject.backend.dto.OrderDTO;
import com.graduationproject.backend.dto.OrderItemDTO;
import com.graduationproject.backend.dto.SelectedItemDTO;
import com.graduationproject.backend.entity.*;
import com.graduationproject.backend.entity.enums.OrderStatus; // Đảm bảo import đúng enum đã cập nhật
import com.graduationproject.backend.entity.enums.PaymentMethod;
import com.graduationproject.backend.exception.BadRequestException;
import com.graduationproject.backend.exception.OperationFailedException;
import com.graduationproject.backend.exception.ResourceNotFoundException;
import com.graduationproject.backend.repository.CartRepository;
import com.graduationproject.backend.repository.CartItemRepository;
import com.graduationproject.backend.repository.OrderItemRepository;
import com.graduationproject.backend.repository.OrderRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductService productService;

    // Đảm bảo VnPayService được inject (có thể null nếu không có profile cấu hình VNPay)
    @Autowired(required = false)
    private VnPayService vnPayService;

    @Autowired
    private CartItemRepository cartItemRepository;

    // PaymentService có vẻ dùng cho hoàn tiền, đảm bảo nó được inject nếu logic hủy cần
    @Autowired
    private PaymentService paymentService;

    // Helper map Order Item Entity sang DTO
    private OrderItemDTO mapOrderItemToDTO(OrderItem item) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setOrderItemId(item.getOrderItemId());
        dto.setQuantity(item.getQuantity());
        dto.setPrice(item.getPrice());
        if (item.getProduct() != null) {
            dto.setProductId(item.getProduct().getProductId());
            dto.setProductName(item.getProduct().getName());
            dto.setProductImageUrl(item.getProduct().getPrimaryImageUrl()); // Lấy URL ảnh
        }
        return dto;
    }

    // Helper map Order Entity sang DTO
    public OrderDTO mapOrderToDTO(Order order) {
        if (order == null) return null;
        OrderDTO dto = new OrderDTO();
        dto.setOrderId(order.getOrderId());
        dto.setUserId(order.getUserId());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus().name()); // Dùng name() để lấy tên Enum
        dto.setPaymentMethod(order.getPaymentMethod().name()); // Dùng name()
        dto.setVnpayTransactionId(order.getVnpayTransactionId());
        dto.setCreatedAt(order.getCreatedAt());

        // Ánh xạ thông tin giao hàng
        dto.setFullNameShipping(order.getFullNameShipping());
        dto.setPhoneShipping(order.getPhoneShipping());
        dto.setAddressShipping(order.getAddressShipping());
        dto.setNotes(order.getNotes()); // Trường notes đã được ánh xạ

        // Lấy OrderItems từ DB nếu chưa có (do lazy loading) hoặc nếu đã có
        // Trigger loading items nếu cần thiết
        List<OrderItemDTO> itemDTOs = new ArrayList<>();
        if (order.getOrderItems() != null) {
            // Kích hoạt loading nếu cần thiết, đảm bảo danh sách không null
            order.getOrderItems().size(); // Dòng này giúp kích hoạt lazy loading
            itemDTOs = order.getOrderItems().stream().map(this::mapOrderItemToDTO).collect(Collectors.toList());
        }

        dto.setOrderItems(itemDTOs);
        return dto;
    }

    // Phương thức tạo đơn hàng từ TOÀN BỘ giỏ hàng (nếu vẫn dùng luồng này)
    // Nếu bạn chỉ dùng createOrderFromSelectedItems, có thể bỏ phương thức này
    @Transactional // Rất quan trọng: đảm bảo toàn vẹn đơn hàng và tồn kho
    public OrderDTO createOrderFromCart(long userId, PaymentMethod paymentMethod, String clientIpAddress) {
        // Lấy giỏ hàng
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", userId));
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Không thể tạo đơn hàng từ giỏ hàng rỗng.");
        }

        // 1. Tạo Order trước
        Order order = new Order();
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING); // Trạng thái ban đầu luôn là PENDING
        order.setPaymentMethod(paymentMethod);
        order.setTotalAmount(BigDecimal.ZERO); // Tạm tính tổng tiền = 0
        Order savedOrder = orderRepository.save(order); // Lưu để lấy orderId

        BigDecimal calculatedTotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cart.getItems()) {
            // Tìm Product Entity để kiểm tra tồn kho và lấy giá mới nhất
            Product product = productService.findProductEntityById(cartItem.getProductId());
            int requestedQuantity = cartItem.getQuantity();

            if (product.getStock() < requestedQuantity) {
                // Ném lỗi nếu không đủ tồn kho, transaction sẽ rollback
                throw new OperationFailedException(String.format("Không đủ số lượng tồn kho cho sản phẩm '%s' (ID: %d). Còn lại: %d, Yêu cầu: %d",
                        product.getName(), product.getProductId(), product.getStock(), requestedQuantity));
            }
            // Giảm tồn kho ngay tại thời điểm tạo đơn hàng
            productService.decreaseStock(product.getProductId(), requestedQuantity);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(product);
            orderItem.setQuantity(requestedQuantity);
            orderItem.setPrice(product.getPrice()); // Lấy giá hiện tại của sản phẩm
            orderItems.add(orderItem);

            // Tính tổng tiền
            calculatedTotal = calculatedTotal.add(product.getPrice().multiply(BigDecimal.valueOf(requestedQuantity)));
        }

        // 3. Lưu tất cả Order Items
        orderItemRepository.saveAll(orderItems);

        // 4. Cập nhật tổng tiền chính xác cho Order
        savedOrder.setTotalAmount(calculatedTotal);

        String paymentInfo = null; // Dùng để trả về URL thanh toán VNPay nếu cần
        if (paymentMethod == PaymentMethod.VN_PAY) {
            if (vnPayService == null) {
                // Đảm bảo VNPayService được cấu hình nếu chọn phương thức này
                throw new OperationFailedException("VNPay Service is not configured.");
            }
            // Tạo URL thanh toán sau khi order đã được lưu
            paymentInfo = vnPayService.createPaymentUrl(savedOrder, clientIpAddress);
            // Lưu một chỉ báo tạm thời hoặc ID giao dịch pending
            savedOrder.setVnpayTransactionId("PENDING_VNPAY_" + savedOrder.getOrderId());
        }
        // Lưu order lần cuối với tổng tiền và thông tin thanh toán tạm thời
        Order finalOrder = orderRepository.save(savedOrder);

        // Xóa toàn bộ giỏ hàng sau khi tạo đơn từ toàn bộ giỏ hàng (cho cả COD và VNPay ban đầu)
        // Lưu ý: Nếu dùng createOrderFromSelectedItems, việc xóa giỏ hàng sẽ được xử lý khác
        cartRepository.delete(cart);

        // Trigger loading items trước khi map để đảm bảo DTO có đủ dữ liệu
        finalOrder.getOrderItems().size();
        OrderDTO orderDTO = mapOrderToDTO(finalOrder);

        return orderDTO;
    }

    // Phương thức tạo đơn hàng từ các sản phẩm được chọn trong giỏ hàng
    @Transactional
    public Map<String, Object> createOrderFromSelectedItems(long userId, CreateOrderRequestDTO request, String clientIpAddress) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Đơn hàng phải chứa ít nhất một sản phẩm được chọn.");
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING); // Trạng thái ban đầu là PENDING
        order.setPaymentMethod(request.getPaymentMethod());
        order.setTotalAmount(BigDecimal.ZERO); // Tạm tính tổng tiền = 0

        // Ánh xạ thông tin giao hàng
        order.setFullNameShipping(request.getFullNameShipping());
        order.setPhoneShipping(request.getPhoneShipping());
        order.setAddressShipping(request.getAddressShipping());
        order.setNotes(request.getNotes());

        Order savedOrder = orderRepository.save(order); // Lưu để lấy orderId

        BigDecimal calculatedTotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        // Lấy danh sách productIds duy nhất để tìm product entity một lần
        List<Integer> productIds = request.getItems().stream().map(SelectedItemDTO::getProductId).distinct().collect(Collectors.toList());
        Map<Integer, Product> productMap = productService.findProductsMapByIds(productIds); // Lấy products 1 lần

        // Phí giao hàng, giả định bằng 0 như trong mã frontend
        BigDecimal shippingFee = BigDecimal.valueOf(0);

        for (SelectedItemDTO selectedItem : request.getItems()) {
            int productId = selectedItem.getProductId();
            int requestedQuantity = selectedItem.getQuantity();
            if (requestedQuantity <= 0) continue; // Bỏ qua nếu số lượng không hợp lệ

            Product product = productMap.get(productId);
            if (product == null) {
                // Ném lỗi nếu không tìm thấy sản phẩm, transaction sẽ rollback
                throw new ResourceNotFoundException("Sản phẩm", "ID", productId);
            }
            if (product.getStock() < requestedQuantity) {
                // Ném lỗi nếu không đủ tồn kho, transaction sẽ rollback
                throw new OperationFailedException( String.format("Không đủ số lượng tồn kho cho sản phẩm '%s' (ID: %d). Còn lại: %d, Yêu cầu: %d", product.getName(), product.getProductId(), product.getStock(), requestedQuantity) );
            }

            // Giảm tồn kho ngay tại thời điểm tạo đơn hàng
            productService.decreaseStock(productId, requestedQuantity);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(product); // Liên kết với Product entity
            orderItem.setQuantity(requestedQuantity);
            orderItem.setPrice(product.getPrice()); // Lấy giá hiện tại của sản phẩm
            orderItems.add(orderItem);
            // Chỉ cộng giá sản phẩm vào tổng
            calculatedTotal = calculatedTotal.add(product.getPrice().multiply(BigDecimal.valueOf(requestedQuantity)));
        }

        calculatedTotal = calculatedTotal.add(shippingFee); // Cộng phí ship một lần
        if (orderItems.isEmpty()) {
            // Nếu sau khi lọc mà không còn item hợp lệ nào
            throw new BadRequestException("Không có sản phẩm hợp lệ trong đơn hàng được chọn.");
        }

        // Lưu tất cả Order Items
        orderItemRepository.saveAll(orderItems);
        // Cập nhật tổng tiền chính xác cho Order
        savedOrder.setTotalAmount(calculatedTotal);

        String paymentInfo = null;
        if (request.getPaymentMethod() == PaymentMethod.VN_PAY && vnPayService != null) {
            // Tạo URL thanh toán sau khi order đã được lưu
            paymentInfo = vnPayService.createPaymentUrl(savedOrder, clientIpAddress);
            // Lưu một chỉ báo tạm thời hoặc ID giao dịch pending cho VNPay
            savedOrder.setVnpayTransactionId("PENDING_VNPAY_" + savedOrder.getOrderId());
        } else if (request.getPaymentMethod() == PaymentMethod.COD) {
            // Đối với COD, không cần set vnpayTransactionId
            savedOrder.setVnpayTransactionId(null);
            // Logic xóa sản phẩm khỏi giỏ hàng cho COD được xử lý ở Frontend CheckoutPage
            // (Như đã thấy trong mã frontend bạn cung cấp ban đầu)
            // Hoặc bạn có thể thêm logic xóa tại đây nếu muốn backend xử lý cho COD cũng được
        }

        // Lưu order lần cuối với tổng tiền và thông tin thanh toán
        Order finalOrder = orderRepository.save(savedOrder);


        // Đảm bảo order items được load trước khi map sang DTO (tránh LazyInitializationException)
        finalOrder.getOrderItems().size();
        OrderDTO orderDTO = mapOrderToDTO(finalOrder);

        Map<String, Object> result = new HashMap<>(); // Sử dụng HashMap để trả về kết quả
        result.put("order", orderDTO);
        if (paymentInfo != null) { // Chỉ thêm paymentInfo nếu là phương thức VNPay
            result.put("paymentInfo", paymentInfo);
        }
        return result; // Trả về HashMap chứa OrderDTO và paymentInfo (nếu có)
    }

    // Tìm Order DTO theo ID (để hiển thị thông tin, cần readOnly transaction)
    @Transactional(readOnly = true)
    public OrderDTO findOrderDTOById(int orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", "orderId", orderId));
        // Trigger lazy loading OrderItems bên trong readOnly transaction
        order.getOrderItems().size();
        return mapOrderToDTO(order);
    }

    // Tìm Order Entity theo ID (dùng nội bộ, ví dụ khi cần cập nhật order)
    // Phương thức gọi nó cần có @Transactional
    public Order findOrderEntityById(int orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", "orderId", orderId));
    }


    // Tìm các đơn hàng theo User ID (cần readOnly transaction)
    @Transactional(readOnly = true)
    public Page<OrderDTO> findOrdersByUserId(long userId, Pageable pageable) {
        Page<Order> orderPage = orderRepository.findByUserId(userId, pageable);

        // Map sang Page<OrderDTO>
        // Đảm bảo OrderItems được load nếu DTO cần đến chúng
        orderPage.getContent().forEach(order -> {
            try {
                // Cẩn thận trigger loading trong transaction
                order.getOrderItems().size();
            } catch (Exception e) {
                // Xử lý nếu có lỗi khi load item (hiếm khi xảy ra trong transaction @Transactional(readOnly = true))
                System.err.println("Lỗi lazy loading items cho đơn hàng " + order.getOrderId() + " trong findOrdersByUserId: " + e.getMessage());
            }
        });

        return orderPage.map(this::mapOrderToDTO);
    }

    // Hủy đơn hàng
    @Transactional // Quan trọng: đảm bảo toàn vẹn khi hủy, hoàn kho, hoàn tiền
    public boolean cancelOrder(int orderId, String reason) {
        // Lấy entity order bên trong transaction
        Order order = findOrderEntityById(orderId);
        OrderStatus currentStatus = order.getStatus();

        // *** Áp dụng quy tắc hủy đơn hàng ***
        // Chỉ cho phép hủy đơn hàng ở các trạng thái PENDING, PAID, PROCESSING
        final EnumSet<OrderStatus> cancellableStatuses = EnumSet.of(
                OrderStatus.PENDING,
                OrderStatus.PAID,
                OrderStatus.PROCESSING
        );

        if (cancellableStatuses.contains(currentStatus)) {
            // Đặt trạng thái thành CANCELED
            order.setStatus(OrderStatus.CANCELED);
            // Lưu lý do hủy
            order.setCancellationReason(reason);
            // Không cần save ngay, transaction sẽ commit cuối phương thức nếu không có exception

            // Hoàn lại tồn kho cho các sản phẩm trong đơn hàng
            try {
                order.getOrderItems().size(); // Trigger loading order items nếu cần
                for (OrderItem item : order.getOrderItems()) {
                    productService.increaseStock(item.getProduct().getProductId(), item.getQuantity());
                }
                System.out.println("Đã hoàn lại tồn kho cho đơn hàng " + orderId + " bị hủy.");
            } catch (Exception e) {
                // Nếu hoàn kho thất bại, ném exception để transaction rollback
                throw new OperationFailedException("Lỗi khi hoàn lại tồn kho cho đơn hàng bị hủy " + orderId + ". Quá trình hủy bị rollback.", e);
            }

            // Hoàn tiền nếu đơn hàng đã được thanh toán (ví dụ: qua VNPay)
            if (currentStatus == OrderStatus.PAID) {
                // Cần triển khai PaymentService.refundPayment
                boolean refundSuccess = paymentService.refundPayment(order.getVnpayTransactionId(), order.getOrderId(), order.getTotalAmount());
                if (!refundSuccess) {
                    // Nếu hoàn tiền thất bại, ném exception để rollback transaction
                    // (không hủy đơn hàng, không hoàn kho, trạng thái giữ nguyên PAID)
                    throw new OperationFailedException("Hoàn tiền thất bại cho đơn hàng " + orderId + ". Quá trình hủy bị rollback.");
                }
                System.out.println("Đã gửi yêu cầu hoàn tiền cho đơn hàng " + orderId + " bị hủy.");
                // Nếu hoàn tiền thành công, transaction sẽ commit ở cuối
            }

            return true; // Hủy thành công
        } else {
            // Nếu trạng thái không cho phép hủy
            throw new BadRequestException(String.format("Không thể hủy đơn hàng với trạng thái hiện tại: %s. Chỉ cho phép hủy ở trạng thái: %s",
                    currentStatus, cancellableStatuses));
        }
    }


    @Transactional // Rất quan trọng: đảm bảo toàn vẹn khi xử lý kết quả VNPay, cập nhật status, xóa giỏ hàng, hoàn kho
    public OrderDTO handleVnpayReturn(Map<String, String> vnpayParams) {
        // --- BẮT ĐẦU LOGIC XỬ LÝ KẾT QUẢ VNPay ---
        // Bước 1: Xác thực chữ ký trả về từ VNPay
        if (vnPayService == null) {
            throw new OperationFailedException("VNPay Service is not configured.");
        }
        if (!vnPayService.validateReturnSignature(vnpayParams)) {
            // Nếu chữ ký không hợp lệ, ném exception để transaction rollback
            throw new BadRequestException("Chữ ký trả về VNPay không hợp lệ.");
        }

        String vnp_TxnRef = vnpayParams.get("vnp_TxnRef"); // Mã đơn hàng (Order ID)
        String vnp_ResponseCode = vnpayParams.get("vnp_ResponseCode"); // Mã phản hồi từ VNPay
        String vnp_TransactionNo = vnpayParams.get("vnp_TransactionNo"); // Mã giao dịch VNPay (nếu thành công)
        // Có thể lấy thêm các tham số khác nếu muốn lưu chi tiết giao dịch (vnp_Amount, vnp_BankCode, vnp_PayDate, vnp_CardType...)

        int orderId;
        try {
            orderId = Integer.parseInt(vnp_TxnRef);
        } catch (NumberFormatException e) {
            // Ném lỗi nếu Order ID không hợp lệ, transaction rollback
            throw new BadRequestException("Định dạng ID đơn hàng không hợp lệ trong phản hồi VNPay: " + vnp_TxnRef);
        }

        // Lấy entity order bên trong transaction
        Order order = findOrderEntityById(orderId);

        // Kiểm tra để tránh xử lý lặp lại hoặc xử lý đơn hàng đã ở trạng thái cuối cùng
        // Nếu đơn hàng không ở trạng thái PENDING (trạng thái ban đầu khi tạo đơn VNPay),
        // có thể nó đã được xử lý bởi IPN hoặc xử lý lại, chỉ trả về trạng thái hiện tại.
        if (order.getStatus() != OrderStatus.PENDING) {
            System.out.println("Đơn hàng " + orderId + " đã được xử lý hoặc không ở trạng thái PENDING (" + order.getStatus() + ") khi nhận phản hồi VNPay. Bỏ qua cập nhật.");
            // Trigger loading items trước khi map để đảm bảo DTO đầy đủ
            order.getOrderItems().size();
            return mapOrderToDTO(order);
        }

        // Bước 2: Kiểm tra mã phản hồi VNPay
        if ("00".equals(vnp_ResponseCode)) {
            // Giao dịch thành công -> Cập nhật trạng thái thành PAID
            order.setStatus(OrderStatus.PAID);
            order.setVnpayTransactionId(vnp_TransactionNo); // Lưu Mã giao dịch VNPay chính thức

            System.out.println("Thanh toán thành công cho đơn hàng: " + orderId);

            // --- BƯỚC 3: XÓA SẢN PHẨM KHỎI GIỎ HÀNG KHI THANH TOÁN VNPay THÀNH CÔNG ---
            try {
                long userId = order.getUserId();
                // Tìm giỏ hàng của người dùng tương ứng
                Cart userCart = cartRepository.findByUserId(userId).orElse(null); // Giỏ hàng có thể không tồn tại

                if (userCart != null) {
                    // Lấy danh sách Product ID từ các Order Item của đơn hàng này
                    List<Integer> orderedProductIds = order.getOrderItems().stream()
                            .map(item -> item.getProduct().getProductId())
                            .collect(Collectors.toList());

                    // Lọc các CartItem trong giỏ hàng mà có Product ID nằm trong danh sách đã đặt
                    List<CartItem> itemsToRemove = userCart.getItems().stream()
                            .filter(cartItem -> orderedProductIds.contains(cartItem.getProductId()))
                            .collect(Collectors.toList());

                    if (!itemsToRemove.isEmpty()) {
                        // Xóa các CartItem này khỏi danh sách trong entity Cart
                        userCart.getItems().removeAll(itemsToRemove);
                        // Xóa các entity CartItem khỏi repository (xóa khỏi DB)
                        cartItemRepository.deleteAll(itemsToRemove);
                        // Lưu lại entity Cart đã được cập nhật danh sách items (có thể không cần thiết nếu quan hệ @OneToMany đủ cấu hình Cascade)
                        cartRepository.save(userCart);

                        System.out.println("Đã xóa " + itemsToRemove.size() + " sản phẩm khỏi giỏ hàng cho đơn hàng thanh toán thành công " + orderId);
                    } else {
                        System.out.println("Không có sản phẩm nào cần xóa khỏi giỏ hàng cho đơn hàng " + orderId + " hoặc không tìm thấy sản phẩm trong giỏ.");
                    }
                } else {
                    System.out.println("Không tìm thấy giỏ hàng cho người dùng " + userId + ". Không thể xóa sản phẩm.");
                }

            } catch (Exception e) {
                // Log lỗi nếu việc xóa giỏ hàng gặp vấn đề, nhưng không ném exception
                // để không rollback transaction đã cập nhật trạng thái PAID
                System.err.println("Lỗi khi xóa sản phẩm khỏi giỏ hàng sau khi thanh toán VNPay thành công cho đơn hàng " + orderId + ": " + e.getMessage());
            }


        } else {
            // Giao dịch thất bại -> Cập nhật trạng thái thành CANCELED
            order.setStatus(OrderStatus.CANCELED);
            // Tùy chọn lưu mã lỗi VNPay hoặc thông tin thất bại
            // order.setVnpayTransactionId("FAILED_VNPAY_" + vnp_ResponseCode); // Ví dụ

            System.out.println("Thanh toán thất bại cho đơn hàng: " + orderId + ", Mã phản hồi VNPay: " + vnp_ResponseCode);

            // --- BƯỚC 3: HOÀN LẠI TỒN KHO KHI THANH TOÁN VNPay THẤT BẠI ---
            try {
                order.getOrderItems().size(); // Đảm bảo order items được load để hoàn kho
                for (OrderItem item : order.getOrderItems()) {
                    productService.increaseStock(item.getProduct().getProductId(), item.getQuantity());
                }
                System.out.println("Đã hoàn lại tồn kho cho đơn hàng thanh toán thất bại: " + orderId);
            } catch (Exception e) {
                // Nếu hoàn kho thất bại, ném exception để transaction rollback
                // Đảm bảo trạng thái đơn hàng không bị cập nhật thành CANCELED nếu tồn kho không được trả lại
                throw new OperationFailedException("Lỗi khi hoàn lại tồn kho cho đơn hàng VNPay thất bại " + orderId + ". Transaction bị rollback.", e);
            }
        }

        // Lưu lại order với trạng thái (và VNP ID/lý do hủy nếu có) đã cập nhật
        orderRepository.save(order);

        // Trigger loading items trước khi map sang DTO để đảm bảo dữ liệu đầy đủ
        order.getOrderItems().size();
        // --- KẾT THÚC LOGIC XỬ LÝ KẾT QUẢ VNPay ---

        // Trả về DTO của order đã được cập nhật
        return mapOrderToDTO(order);
    }


    // --- Phương thức cập nhật trạng thái bởi Admin ---
    // Định nghĩa các trạng thái cho phép chuyển đổi
    private static final EnumSet<OrderStatus> CAN_UPDATE_FROM_PENDING = EnumSet.of(OrderStatus.PAID, OrderStatus.PROCESSING, OrderStatus.CANCELED);
    private static final EnumSet<OrderStatus> CAN_UPDATE_FROM_PAID = EnumSet.of(OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.CANCELED);
    private static final EnumSet<OrderStatus> CAN_UPDATE_FROM_PROCESSING = EnumSet.of(OrderStatus.SHIPPED, OrderStatus.CANCELED);
    private static final EnumSet<OrderStatus> CAN_UPDATE_FROM_SHIPPED = EnumSet.of(OrderStatus.COMPLETED); // Chỉ cho phép chuyển từ SHIPPED sang COMPLETED
    private static final EnumSet<OrderStatus> FINAL_STATUSES = EnumSet.of(OrderStatus.COMPLETED, OrderStatus.CANCELED); // Các trạng thái cuối cùng

    @Transactional // Quan trọng
    public OrderDTO updateOrderStatus(int orderId, OrderStatus newStatus) {
        Order order = findOrderEntityById(orderId); // Lấy entity bên trong transaction
        OrderStatus currentStatus = order.getStatus();

        // Không làm gì nếu trạng thái mới giống trạng thái cũ
        if (currentStatus == newStatus) {
            order.getOrderItems().size(); // Load items trước khi map
            return mapOrderToDTO(order);
        }

        // Kiểm tra logic chuyển trạng thái hợp lệ
        boolean isValidTransition = false;
        switch (currentStatus) {
            case PENDING:    isValidTransition = CAN_UPDATE_FROM_PENDING.contains(newStatus); break;
            case PAID:       isValidTransition = CAN_UPDATE_FROM_PAID.contains(newStatus); break;
            case PROCESSING: isValidTransition = CAN_UPDATE_FROM_PROCESSING.contains(newStatus); break; // Check từ PROCESSING
            case SHIPPED:    isValidTransition = CAN_UPDATE_FROM_SHIPPED.contains(newStatus); break;
            case COMPLETED:
            case CANCELED:   isValidTransition = false; break; // Không đổi từ các trạng thái cuối cùng
        }

        if (!isValidTransition) {
            throw new BadRequestException(String.format("Không thể thay đổi trạng thái đơn hàng từ %s sang %s", currentStatus, newStatus));
        }

        // Xử lý các hành động đặc biệt khi chuyển trạng thái
        if (newStatus == OrderStatus.CANCELED && currentStatus != OrderStatus.CANCELED) {
            // Nếu chuyển sang CANCELED (và chưa ở trạng thái CANCELED)
            // Hoàn lại tồn kho
            try {
                order.getOrderItems().size(); // Trigger loading
                for (OrderItem item : order.getOrderItems()) {
                    productService.increaseStock(item.getProduct().getProductId(), item.getQuantity());
                }
                System.out.println("[Admin] Đã hoàn lại tồn kho khi Admin hủy đơn hàng " + orderId + ".");
            } catch (Exception e) {
                throw new OperationFailedException("Lỗi khi hoàn lại tồn kho khi Admin hủy đơn hàng " + orderId + ". Cập nhật trạng thái bị rollback.", e);
            }
            // TODO: Thêm logic hoàn tiền nếu trạng thái cũ là PAID
            if (currentStatus == OrderStatus.PAID) {
                // Cần gọi paymentService.refundPayment(...) tương tự như trong cancelOrder()
                boolean refundSuccess = paymentService.refundPayment(order.getVnpayTransactionId(), order.getOrderId(), order.getTotalAmount());
                if (!refundSuccess) {
                    throw new OperationFailedException("Hoàn tiền thất bại khi Admin hủy đơn hàng " + orderId + ". Cập nhật trạng thái bị rollback.");
                }
                System.out.println("[Admin] Đã gửi yêu cầu hoàn tiền cho đơn hàng " + orderId + " bị Admin hủy.");
            }
        }
        // TODO: Thêm logic khác khi chuyển sang các trạng thái khác (ví dụ: gửi email thông báo khi SHIPPED)


        // Cập nhật trạng thái mới
        order.setStatus(newStatus);

        // Lưu lại order đã cập nhật
        // orderRepository.save(order); // Không cần gọi save() tường minh trong @Transactional nếu entity đã managed

        // Trigger loading items trước khi map
        order.getOrderItems().size();
        // Trả về DTO của order đã cập nhật
        return mapOrderToDTO(order);
    }

    // Tìm kiếm đơn hàng theo User ID và/hoặc Status (dùng cho User hoặc Admin lọc)
    @Transactional(readOnly = true)
    public Page<OrderDTO> searchOrdersByUser(Long userId, OrderStatus status, Pageable pageable) {
        Specification<Order> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (userId != null && userId > 0) { // Kiểm tra userId hợp lệ
                predicates.add(criteriaBuilder.equal(root.get("userId"), userId));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            // Thêm sắp xếp mặc định nếu chưa có
            if (query.getOrderList().isEmpty()) {
                query.orderBy(criteriaBuilder.desc(root.get("createdAt")));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        // findAll với Specification sẽ trả về Page các entity Order
        Page<Order> orderPage = orderRepository.findAll(specification, pageable);
        // Trigger loading items cho từng entity trong page trước khi map
        orderPage.getContent().forEach(order -> order.getOrderItems().size());
        // Map Page các entity Order sang Page các DTO OrderDTO
        return orderPage.map(this::mapOrderToDTO);
    }

    // Tìm kiếm tất cả đơn hàng (dùng cho Admin, có thể lọc theo status hoặc userId)
    @Transactional(readOnly = true)
    public Page<OrderDTO> findAllOrdersForAdmin(OrderStatus statusFilter, Long userIdFilter, Pageable pageable) {
        // Sử dụng Specification để tạo điều kiện lọc động (linh hoạt hơn)
        Specification<Order> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (statusFilter != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), statusFilter));
            }
            if (userIdFilter != null && userIdFilter > 0) {
                predicates.add(criteriaBuilder.equal(root.get("userId"), userIdFilter));
            }
            // Sắp xếp mặc định theo ngày tạo giảm dần nếu chưa có sắp xếp
            if (query.getOrderList().isEmpty()) {
                query.orderBy(criteriaBuilder.desc(root.get("createdAt")));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        // Tìm tất cả đơn hàng dựa trên Specification và Pageable
        Page<Order> orderPage = orderRepository.findAll(spec, pageable);
        // Trigger loading OrderItems cho từng entity trong page trước khi map
        orderPage.getContent().forEach(order -> order.getOrderItems().size());
        // Map Page các entity Order sang Page các DTO OrderDTO
        return orderPage.map(this::mapOrderToDTO);
    }
}