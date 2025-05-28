package com.graduationproject.backend.service;

import com.graduationproject.backend.dto.InvoiceDetailDTO;
import com.graduationproject.backend.dto.OrderDTOForInvoice;
import com.graduationproject.backend.dto.OrderItemDTOForInvoice;
import com.graduationproject.backend.entity.Invoice;
import com.graduationproject.backend.entity.Order;
import com.graduationproject.backend.entity.User;
import com.graduationproject.backend.exception.BadRequestException; // Import nếu cần check trạng thái Order
import com.graduationproject.backend.exception.ResourceNotFoundException;
import com.graduationproject.backend.repository.InvoiceRepository;
import com.graduationproject.backend.repository.OrderItemRepository;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import Transactional

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional; // Import Optional nếu dùng findByOrderOrderId trả về Optional
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;
    @Autowired
    private UserService userService;
    // Inject OrderService để lấy thông tin Order
    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderItemRepository orderItemRepository; // Inject nếu cần tính toán lại subtotal

    private InvoiceDetailDTO mapInvoiceToDetailDTO(Invoice invoice) {
        if (invoice == null || invoice.getOrder() == null) {
            // ensureInvoiceExists nên đã xử lý việc Order không tồn tại
            throw new ResourceNotFoundException("Invoice or associated Order not found for mapping.");
        }

        InvoiceDetailDTO dto = new InvoiceDetailDTO();
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setCreatedAt(invoice.getCreatedAt()); // Ngày xuất hóa đơn
        dto.getStoreName();
        dto.getStoreEmail();
        dto.getStoreAddress();
        dto.getStorePhone();
        dto.getStoreTaxCode();

        Order orderEntity = invoice.getOrder();
        Hibernate.initialize(orderEntity.getOrderItems()); // Ví dụ
        orderEntity.getOrderItems().forEach(item -> Hibernate.initialize(item.getProduct()));

        OrderDTOForInvoice orderDTO = new OrderDTOForInvoice();
        orderDTO.setOrderId(orderEntity.getOrderId());
        orderDTO.setOrderCreatedAt(orderEntity.getCreatedAt());
        orderDTO.setCustomerFullName(orderEntity.getFullNameShipping());
        orderDTO.setCustomerAddress(orderEntity.getAddressShipping());
        orderDTO.setCustomerPhone(orderEntity.getPhoneShipping());
        orderDTO.setNotes(orderEntity.getNotes());


        User customer = userService.findById(orderEntity.getUserId())
                .orElse(null);
        orderDTO.setUserId(orderEntity.getUserId());
        if (customer != null) {
            orderDTO.setCustomerEmail(customer.getEmail());
        } else {
            orderDTO.setCustomerEmail("N/A");
        }

        // Map OrderItems
        List<OrderItemDTOForInvoice> itemDTOs = orderEntity.getOrderItems().stream().map(oi -> {
            OrderItemDTOForInvoice itemDTO = new OrderItemDTOForInvoice();
            // Đảm bảo Product được load (nếu nó lazy trong OrderItem)
            // Nếu Product là EAGER trong OrderItem hoặc đã được fetch join, thì không cần initialize
            // Hibernate.initialize(oi.getProduct()); // Cân nhắc nếu cần
            itemDTO.setProductName(oi.getProduct() != null ? oi.getProduct().getName() : "N/A");
            itemDTO.setProductSku(oi.getProduct() != null ? oi.getProduct().getBarcode() : "N/A"); // Ví dụ SKU là barcode
            itemDTO.setQuantity(oi.getQuantity());
            itemDTO.setPrice(oi.getPrice()); // Giá tại thời điểm mua
            return itemDTO;
        }).collect(Collectors.toList());
        orderDTO.setOrderItems(itemDTOs);

        // Tính toán subTotal, shippingFee, discountAmount (nếu có)
        BigDecimal subTotal = BigDecimal.ZERO;
        for (OrderItemDTOForInvoice item : itemDTOs) {
            subTotal = subTotal.add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        orderDTO.setSubTotal(subTotal);
        // Giả sử shippingFee và discountAmount được lưu trong Order entity hoặc tính toán từ logic khác
        orderDTO.setShippingFee(BigDecimal.ZERO); // Ví dụ, cần lấy từ Order entity nếu có
        orderDTO.setDiscountAmount(BigDecimal.ZERO); // Ví dụ

        orderDTO.setTotalAmount(orderEntity.getTotalAmount());
        orderDTO.setPaymentMethod(orderEntity.getPaymentMethod().name());

        dto.setOrder(orderDTO);
        // Thông tin cửa hàng có thể set mặc định trong DTO hoặc lấy từ config
        // dto.setStoreName("TimeXpert Store"); // Đã làm trong DTO

        return dto;
    }
    @Transactional
public InvoiceDetailDTO getInvoiceDetails(int orderId) {
    // Bước 1: Đảm bảo hóa đơn tồn tại (có thể tạo mới nếu chưa có)
    // Phương thức này sẽ chạy trong transaction riêng của nó (nếu được gọi từ ngoài)
    // hoặc transaction của getInvoiceDetails (nếu getInvoiceDetails có @Transactional)
    Invoice invoice = ensureInvoiceExists(orderId); // Gọi phương thức có khả năng ghi

    // Bước 2: Map sang DTO (phần này chỉ đọc)
    // Bây giờ chúng ta có thể thực hiện các thao tác fetch và map một cách an toàn
    // vì invoice đã được đảm bảo là tồn tại và được quản lý bởi persistence context.
    // Tuy nhiên, để an toàn nhất cho các lazy loading, việc mapping nên nằm trong 1 transaction.
    // => Đặt @Transactional cho getInvoiceDetails (không readOnly) là hợp lý nhất
    //    để cả ensureInvoiceExists và mapInvoiceToDetailDTO đều trong cùng 1 transaction.
    return mapInvoiceToDetailDTO(invoice); // Phương thức map đã viết ở câu trả lời trước
}
    @Transactional // Mặc định readOnly = false
    public Invoice ensureInvoiceExists(int orderId) {
        Invoice existingInvoice = invoiceRepository.findByOrderOrderId((long) orderId);
        if (existingInvoice != null) {
            return existingInvoice;
        }

        // Nếu chưa tồn tại, tạo Invoice mới
        Order order = orderService.findOrderEntityById(orderId); // Service này nên ném lỗi nếu order không thấy

        Invoice newInvoice = new Invoice();
        newInvoice.setOrder(order);
        newInvoice.setTotalAmount(order.getTotalAmount());
        newInvoice.setInvoiceNumber("INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        // createdAt sẽ tự động được gán bởi @CreationTimestamp

        return invoiceRepository.save(newInvoice); // << THAO TÁC GHI
    }
//    @Transactional // Đảm bảo toàn vẹn
//    public Invoice generateOrGetInvoiceForOrder(int orderId) {
//        // 1. Kiểm tra xem hóa đơn đã tồn tại cho orderId này chưa
//        // Ép kiểu int sang long khi gọi phương thức repository
//        Invoice existing = invoiceRepository.findByOrderOrderId((long) orderId);
//        if (existing != null) {
//            // Nếu đã tồn tại, fetch Order để đảm bảo có đủ thông tin trả về (nếu cần)
//            existing.getOrder().getOrderItems().size(); // Trigger loading nếu cần
//            return existing;
//        }
//
//        // 2. Nếu chưa tồn tại, lấy Order entity
//        Order order = orderService.findOrderEntityById(orderId); // Hàm này đã xử lý ResourceNotFoundException
//
//        // 3. (Tùy chọn) Kiểm tra trạng thái Order trước khi tạo hóa đơn
//        // Ví dụ: Chỉ cho phép tạo hóa đơn khi đã thanh toán hoặc hoàn thành
//        // if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.COMPLETED) {
//        //     throw new BadRequestException("Cannot generate invoice for order with status: " + order.getStatus());
//        // }
//
//        // 4. Tạo Invoice mới
//        Invoice invoice = new Invoice();
//        invoice.setOrder(order); // Gán đối tượng Order đầy đủ
//        invoice.setTotalAmount(order.getTotalAmount());
//        invoice.setInvoiceNumber("INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
//
//        // 5. Lưu và trả về Invoice mới
//        return invoiceRepository.save(invoice);
//    }
//
//    /**
//     * Lấy hóa đơn đã tồn tại theo Order ID.
//     * @param orderId ID của Order
//     * @return Invoice nếu tìm thấy, null nếu không tồn tại.
//     */
//    @Transactional(readOnly = true) // Chỉ đọc dữ liệu
//    public Invoice getExistingInvoiceByOrderId(int orderId) {
//        Invoice invoice = invoiceRepository.findByOrderOrderId((long) orderId);
//        if (invoice != null) {
//            // Fetch Order để đảm bảo có đủ thông tin trả về (nếu cần)
//            invoice.getOrder().getOrderItems().size(); // Trigger loading nếu cần
//        }
//        return invoice; // Trả về invoice hoặc null
//    }
}