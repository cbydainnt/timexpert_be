// src/main/java/com/graduationproject/backend/service/CartService.java
package com.graduationproject.backend.service;

import com.graduationproject.backend.dto.CartDTO;
import com.graduationproject.backend.dto.CartItemDetailDTO;
import com.graduationproject.backend.entity.Cart;
import com.graduationproject.backend.entity.CartItem;
import com.graduationproject.backend.entity.Product;
import com.graduationproject.backend.exception.BadRequestException;
import com.graduationproject.backend.exception.OperationFailedException;
import com.graduationproject.backend.exception.ResourceNotFoundException;
import com.graduationproject.backend.repository.CartRepository; // JPA Cart Repo
import com.graduationproject.backend.repository.CartItemRepository; // JPA CartItem Repo
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp; // *** THÊM IMPORT Timestamp ***
import java.time.Instant; // *** THÊM IMPORT Instant ***
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hibernate.Hibernate; // Import Hibernate để kiểm tra lazy loading

@Service
public class CartService {

    @Autowired private CartRepository cartRepository;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private ProductService productService;

    @Transactional
    protected Cart getOrCreateCartEntity(long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            Cart newCart = new Cart();
            newCart.setUserId(userId);
            return cartRepository.save(newCart);
        });
    }

    @Transactional(readOnly = true)
    public Optional<CartDTO> getCartByUserIdWithDetails(long userId) {
        Optional<Cart> cartOpt = cartRepository.findByUserId(userId);
        if (cartOpt.isEmpty()) return Optional.empty();

        Cart cart = cartOpt.get();
        // Lấy CartItem entities từ Cart (nếu là EAGER hoặc trong transaction)
        List<CartItem> cartItems = new ArrayList<>(cart.getItems());
        if (cartItems.isEmpty()) {
            // Trả về CartDTO rỗng nếu cart tồn tại nhưng không có item
            return Optional.of(new CartDTO(userId, Collections.emptyList(), cart.getLastUpdated()));
        }

        List<Integer> productIds = cartItems.stream().map(CartItem::getProductId).distinct().collect(Collectors.toList());
        Map<Integer, Product> productMap = productService.findProductsMapByIds(productIds); // Cần hàm này trong ProductService

        List<CartItemDetailDTO> detailDTOs = cartItems.stream()
                .map(cartItem -> {
                    Product product = productMap.get(cartItem.getProductId());
                    if (product == null) return null;
                    return CartItemDetailDTO.builder()
                            .productId(cartItem.getProductId())
                            .quantity(cartItem.getQuantity())
                            .price(product.getPrice())
                            .name(product.getName())
                            .imageUrl(product.getPrimaryImageUrl())
                            .stock(product.getStock())
                            .build();
                })
                .filter(Objects::nonNull).collect(Collectors.toList());

        // Không cần kiểm tra detailDTOs.isEmpty() nữa vì đã kiểm tra cartItems ở trên

        CartDTO cartDTO = new CartDTO(userId, detailDTOs, cart.getLastUpdated());
        return Optional.of(cartDTO);
    }

    @Transactional
    public CartDTO addItemToCart(long userId, int productId, int quantity) {
        if (quantity <= 0) { throw new BadRequestException("Quantity must be positive."); }
        Product product = productService.findProductEntityById(productId);
        int availableStock = product.getStock();
        Cart cart = getOrCreateCartEntity(userId);

        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartCartIdAndProductId(cart.getCartId(), productId);
        int currentQuantityInCart = existingItemOpt.map(CartItem::getQuantity).orElse(0);
        if (currentQuantityInCart + quantity > availableStock) {
            throw new OperationFailedException(String.format("Only %d available for '%s'. Cannot add %d.", availableStock, product.getName(), quantity));
        }

        CartItem itemToSave;
        if (existingItemOpt.isPresent()) {
            itemToSave = existingItemOpt.get();
            itemToSave.setQuantity(currentQuantityInCart + quantity);
            itemToSave.setPriceAtAddition(product.getPrice());
        } else {
            itemToSave = new CartItem();
            itemToSave.setCart(cart);
            itemToSave.setProductId(productId);
            itemToSave.setQuantity(quantity);
            itemToSave.setPriceAtAddition(product.getPrice());
        }
        cartItemRepository.save(itemToSave);

        // *** SỬA LẠI: Dùng Timestamp ***
        cart.setLastUpdated(Timestamp.from(Instant.now())); // Hoặc new Timestamp(System.currentTimeMillis())
        cartRepository.save(cart);

        return getCartByUserIdWithDetails(userId).orElseThrow(() -> new OperationFailedException("Cart not found after add."));
    }

    @Transactional
    public CartDTO updateItemQuantity(long userId, int productId, int quantity) {
        if (quantity <= 0) { return removeItemFromCart(userId, productId); }

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", userId));
        CartItem itemToUpdate = cartItemRepository.findByCartCartIdAndProductId(cart.getCartId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "productId", productId));

        Product product = productService.findProductEntityById(productId);
        if (quantity > product.getStock()) {
            throw new OperationFailedException(String.format("Only %d available for '%s'. Cannot update quantity to %d.", product.getStock(), product.getName(), quantity));
        }

        itemToUpdate.setQuantity(quantity);
        itemToUpdate.setPriceAtAddition(product.getPrice());
        cartItemRepository.save(itemToUpdate);

        // *** SỬA LẠI: Dùng Timestamp ***
        cart.setLastUpdated(Timestamp.from(Instant.now()));
        cartRepository.save(cart);

        return getCartByUserIdWithDetails(userId).orElseThrow(() -> new OperationFailedException("Cart not found after update."));
    }

//    @Transactional
//    public CartDTO removeItemFromCart(long userId, int productId) {
//        Cart cart = cartRepository.findByUserId(userId)
//                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", userId));
//
//        // *** SỬA LẠI: Tìm item trước rồi xóa ***
//        CartItem itemToRemove = cartItemRepository.findByCartCartIdAndProductId(cart.getCartId(), productId)
//                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "productId", productId + " not found in cart"));
//
//        cartItemRepository.delete(itemToRemove); // Xóa item tìm được
//
//        // *** SỬA LẠI: Kiểm tra cart rỗng bằng cách load lại và check size ***
//        // Load lại cart từ DB để lấy trạng thái mới nhất của list items
//        Cart updatedCart = cartRepository.findById(cart.getCartId()).orElse(null);
//
//        if (updatedCart == null || !Hibernate.isInitialized(updatedCart.getItems()) || updatedCart.getItems().isEmpty()) {
//            if (updatedCart != null) cartRepository.delete(updatedCart); // Xóa luôn Cart nếu rỗng
//            return new CartDTO(userId, Collections.emptyList(), new Date()); // Trả về cart DTO rỗng
//        } else {
//            // *** SỬA LẠI: Dùng Timestamp ***
//            updatedCart.setLastUpdated(Timestamp.from(Instant.now()));
//            cartRepository.save(updatedCart);
//            // Trả về CartDTO mới nhất (load lại lần nữa để đảm bảo)
//            return getCartByUserIdWithDetails(userId).orElse(new CartDTO(userId, Collections.emptyList(), new Date()));
//        }
//    }

    @Transactional
    public CartDTO removeItemFromCart(long userId, int productId) {
        // Lấy giỏ hàng của người dùng
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", userId));

        // Tìm CartItem cần xóa
        CartItem itemToRemove = cartItemRepository.findByCartCartIdAndProductId(cart.getCartId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "productId", productId + " not found in cart"));

        // Xóa CartItem khỏi danh sách items của cart
        cart.getItems().remove(itemToRemove);

        // Xóa CartItem khỏi cơ sở dữ liệu
        cartItemRepository.delete(itemToRemove);

        // Kiểm tra xem giỏ hàng có rỗng không
        if (cart.getItems().isEmpty()) {
            // Nếu rỗng, xóa luôn Cart
            cartRepository.delete(cart);
            return new CartDTO(userId, Collections.emptyList(), new Date());
        } else {
            // Nếu không rỗng, cập nhật lastUpdated và lưu lại cart
            cart.setLastUpdated(Timestamp.from(Instant.now()));
            cartRepository.save(cart);
            // Trả về CartDTO mới nhất
            return getCartByUserIdWithDetails(userId).orElse(new CartDTO(userId, Collections.emptyList(), new Date()));
        }
    }
    @Transactional
    public void removeItemsFromCartBatch(long userId, List<Integer> productIds) {
        if (productIds == null || productIds.isEmpty()) return;
        Optional<Cart> cartOpt = cartRepository.findByUserId(userId);
        if(cartOpt.isPresent()) {
            Cart cart = cartOpt.get();
            // Gọi hàm xóa batch trong repository
            cartItemRepository.deleteByCartCartIdAndProductIdIn(cart.getCartId(), productIds);

            // *** SỬA LẠI: Kiểm tra cart rỗng bằng cách load lại ***
            Cart updatedCart = cartRepository.findById(cart.getCartId()).orElse(null);
            if (updatedCart == null || !Hibernate.isInitialized(updatedCart.getItems()) || updatedCart.getItems().isEmpty()) {
                if(updatedCart != null) cartRepository.delete(updatedCart);
            } else {
                // *** SỬA LẠI: Dùng Timestamp ***
                updatedCart.setLastUpdated(Timestamp.from(Instant.now()));
                cartRepository.save(updatedCart);
            }
        }
    }

    @Transactional
    public void deleteCart(long userId) {
        // Nên dùng deleteByUserId để tránh phải load Cart entity không cần thiết
        cartRepository.deleteByUserId(userId);
        // Hoặc cách cũ:
        // cartRepository.findByUserId(userId).ifPresent(cartRepository::delete);
    }
}