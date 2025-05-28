package com.graduationproject.backend.controller;

import com.graduationproject.backend.dto.ProductDTO; // Import DTO
import com.graduationproject.backend.dto.ProductPageDTO;
import com.graduationproject.backend.dto.ProductRequestDTO;
import com.graduationproject.backend.entity.Product; // Vẫn cần entity để nhận request body nếu dùng entity trực tiếp
import com.graduationproject.backend.service.FileStorageService;
import com.graduationproject.backend.service.ProductService;
import jakarta.validation.Valid; // Import Valid
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final FileStorageService fileStorageService; // Inject FileStorageService

    @Value("${file.base-url}") // Inject base-url
    private String fileStorageBaseUrl;

    @Autowired
    public ProductController(ProductService productService, FileStorageService fileStorageService) {
        this.productService = productService;
        this.fileStorageService = fileStorageService;
    }
    @GetMapping
    public ResponseEntity<ProductPageDTO> getProducts(
            @RequestParam(defaultValue = "") String name,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortField = "createdAt";
        if (sortBy.equalsIgnoreCase("productId") || sortBy.equalsIgnoreCase("name") || sortBy.equalsIgnoreCase("price") || sortBy.equalsIgnoreCase("stock")) {
            sortField = sortBy;
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        Page<ProductDTO> productDTOPage = productService.findProductsByFilter(name, categoryId, minPrice, maxPrice, pageable);
        ProductPageDTO responseDTO = ProductPageDTO.fromPage(productDTOPage);
        return ResponseEntity.ok(responseDTO);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable int id) {
        ProductDTO productDTO = productService.findById(id);
        return ResponseEntity.ok(productDTO);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDTO> createProduct(@Valid @RequestBody ProductRequestDTO productRequestDTO) { // <<<< THAY ĐỔI Ở ĐÂY
        ProductDTO savedProductDTO = productService.createProductWithImages(productRequestDTO); // <<<< GỌI SERVICE MỚI
        return ResponseEntity.status(HttpStatus.CREATED).body(savedProductDTO);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDTO> updateProduct(@PathVariable int id, @Valid @RequestBody ProductRequestDTO productRequestDTO) { // <<<< THAY ĐỔI Ở ĐÂY
        ProductDTO updatedProductDTO = productService.updateProductWithImages(id, productRequestDTO); // <<<< GỌI SERVICE MỚI
        return ResponseEntity.ok(updatedProductDTO);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable int id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/upload-images")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadProductImages(@RequestParam("files") MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng chọn ít nhất một file để tải lên."));
        }
        List<String> fileUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            try {
                String url = fileStorageService.storeFile(file);
                fileUrls.add(url);
            } catch (RuntimeException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Tải lên file " + file.getOriginalFilename() + " thất bại: " + e.getMessage()));
            }
        }
        if (fileUrls.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Không có file hợp lệ nào được tải lên."));
        }
        return ResponseEntity.ok(fileUrls);
    }
}