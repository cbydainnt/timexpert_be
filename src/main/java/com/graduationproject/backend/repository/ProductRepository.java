package com.graduationproject.backend.repository;

import com.graduationproject.backend.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    // Thêm "LEFT JOIN FETCH p.category c" để load Category cùng lúc
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category c WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) AND p.price BETWEEN :minPrice AND :maxPrice")
    Page<Product> findByNameContainingIgnoreCaseAndPriceBetween(
            @Param("name") String name,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);

    // Thêm "LEFT JOIN FETCH p.category c"
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category c WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) AND c.categoryId = :categoryId AND p.price BETWEEN :minPrice AND :maxPrice")
    Page<Product> findByNameContainingIgnoreCaseAndCategoryCategoryIdAndPriceBetween(
            @Param("name") String name,
            @Param("categoryId") int categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);

    // Phương thức cập nhật tồn kho (an toàn hơn khi dùng optimistic locking nếu cần)
    @Modifying // Cần thiết cho các câu lệnh UPDATE/DELETE
    @Query("UPDATE Product p SET p.stock = p.stock - :quantity WHERE p.productId = :productId AND p.stock >= :quantity")
    int decreaseStock(@Param("productId") int productId, @Param("quantity") int quantity);

    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock + :quantity WHERE p.productId = :productId")
    int increaseStock(@Param("productId") int productId, @Param("quantity") int quantity);

    // Optional: dùng findById nhiều và cũng muốn fetch Category
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category c WHERE p.productId = :id")
    Optional<Product> findByIdWithCategory(@Param("id") int id);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category c WHERE p.productId IN :ids")
    List<Product> findAllByIdWithCategory(@Param("ids") List<Integer> ids);


    //new update
    // Trả về List Object[] { productName, stock }
    @Query("SELECT p.name, p.stock FROM Product p ORDER BY p.stock ASC")
    List<Object[]> getInventorySummary();

    // Tổng số sản phẩm (cho dashboard)
    long count();

    // Optional: Tìm sản phẩm đầu tiên khớp tên (không phân biệt hoa thường)
    Optional<Product> findFirstByNameContainingIgnoreCase(String name);
}