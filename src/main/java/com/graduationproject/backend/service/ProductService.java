package com.graduationproject.backend.service;

import com.graduationproject.backend.dto.ProductDTO;
import com.graduationproject.backend.dto.ProductRequestDTO;
import com.graduationproject.backend.entity.Category;
import com.graduationproject.backend.entity.Product;
import com.graduationproject.backend.entity.ProductImage;
import com.graduationproject.backend.exception.BadRequestException;
import com.graduationproject.backend.exception.OperationFailedException;
import com.graduationproject.backend.exception.ResourceNotFoundException;
import com.graduationproject.backend.repository.CategoryRepository;
import com.graduationproject.backend.repository.ProductImageRepository;
import com.graduationproject.backend.repository.ProductRepository;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // *** Đảm bảo đã import ***
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductImageRepository productImageRepository;
    @Autowired
    private FileStorageService fileStorageService;

    // Hàm helper map Entity sang DTO
    public ProductDTO mapToDTO(Product product) {
        if (product == null) return null;
        ProductDTO dto = new ProductDTO();
        dto.setProductId(product.getProductId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStock(product.getStock());
        dto.setBarcode(product.getBarcode());
        dto.setBrand(product.getBrand());
        dto.setModel(product.getModel());
        dto.setMovement(product.getMovement());
        dto.setCaseMaterial(product.getCaseMaterial());
        dto.setStrapMaterial(product.getStrapMaterial());
        dto.setDialColor(product.getDialColor());
        dto.setWaterResistance(product.getWaterResistance());
        dto.setAverageRating(product.getAverageRating());
        dto.setReviewCount(product.getReviewCount());
        dto.setVisible(product.isVisible());
        dto.setUpdatedAt(product.getUpdatedAt());

        if (Hibernate.isInitialized(product.getImages()) && product.getImages() != null) {
            dto.setPrimaryImageUrl(product.getPrimaryImageUrl());
            dto.setImageUrls(product.getImages().stream()
                    .map(ProductImage::getImageUrl)
                    .collect(Collectors.toList()));
        } else {
            dto.setPrimaryImageUrl(null);
            dto.setImageUrls(Collections.emptyList());
        }

        if (product.getCategory() != null && Hibernate.isInitialized(product.getCategory())) {
            dto.setCategoryId(product.getCategory().getCategoryId());
            dto.setCategoryName(product.getCategory().getName());
        } else if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getCategoryId());
        }
        dto.setCreatedAt(product.getCreatedAt());
        return dto;
    }

    public Page<ProductDTO> mapPageToDTO(Page<Product> productPage) {
        return productPage.map(product -> {
            Hibernate.initialize(product.getImages());
            return mapToDTO(product);
        });
    }

    @Transactional(readOnly = true)
    public Page<ProductDTO> findProductsByFilter(String name, Integer categoryId,
                                                 BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        BigDecimal min = (minPrice == null || minPrice.compareTo(BigDecimal.ZERO) < 0) ? BigDecimal.ZERO : minPrice;
        BigDecimal max = (maxPrice == null || maxPrice.compareTo(BigDecimal.ZERO) < 0) ? new BigDecimal("9999999999.99") : maxPrice;
        String searchName = (name == null ? "" : name.trim().toLowerCase());

        Page<Product> productPage;
        if (categoryId == null) {
            productPage = productRepository.findByNameContainingIgnoreCaseAndPriceBetween(searchName, min, max, pageable);
        } else {
            if (!categoryRepository.existsById(categoryId)) {
                throw new ResourceNotFoundException("Category", "categoryId", categoryId);
            }
            productPage = productRepository.findByNameContainingIgnoreCaseAndCategoryCategoryIdAndPriceBetween(searchName, categoryId, min, max, pageable);
        }
        return mapPageToDTO(productPage);
    }

    @Transactional(readOnly = true)
    public ProductDTO findById(int id) {
        Product product = productRepository.findByIdWithCategory(id) // Sử dụng query đã fetch category
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", id));
        Hibernate.initialize(product.getImages());
        return mapToDTO(product);
    }

    @Transactional(readOnly = true)
    public Product findProductEntityById(int id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", id));
        Hibernate.initialize(product.getCategory());
        Hibernate.initialize(product.getImages());
        return product;
    }

    @Transactional
    public ProductDTO createProductWithImages(ProductRequestDTO dto) {
        if (dto.getCategoryId() == null || dto.getCategoryId() <= 0) {
            throw new BadRequestException("Category ID is required and must be a valid positive number for product creation.");
        }
        Product product = new Product();
        mapDtoToProductEntity(dto, product);

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "ID for creation", dto.getCategoryId()));
        product.setCategory(category);

        Product savedProductEntity = productRepository.saveAndFlush(product);

        updateProductImages(savedProductEntity, dto.getImageUrls(), dto.getPrimaryImageUrl());

        Product finalProduct = productRepository.findByIdWithCategory(savedProductEntity.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", savedProductEntity.getProductId()));
        Hibernate.initialize(finalProduct.getImages());
        return mapToDTO(finalProduct);
    }

    @Transactional
    public ProductDTO updateProductWithImages(int productId, ProductRequestDTO dto) {
        Product product = productRepository.findByIdWithCategory(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));
        Hibernate.initialize(product.getImages());
        List<String> oldImageUrlsOnServer = product.getImages().stream()
                .map(ProductImage::getImageUrl)
                .collect(Collectors.toList());
        mapDtoToProductEntity(dto, product);
        if (dto.getCategoryId() == null || dto.getCategoryId() <= 0) {
            throw new BadRequestException("Category ID is required and must be a valid positive number for update.");
        }
        if (product.getCategory() == null || !dto.getCategoryId().equals(product.getCategory().getCategoryId())) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "ID for update", dto.getCategoryId()));
            product.setCategory(category);
        }

        productRepository.save(product);
        updateProductImages(product, dto.getImageUrls(), dto.getPrimaryImageUrl());

        List<String> newImageUrlsFromDto = dto.getImageUrls() != null ? dto.getImageUrls() : Collections.emptyList();
        oldImageUrlsOnServer.stream()
                .filter(oldUrl -> !newImageUrlsFromDto.contains(oldUrl))
                .forEach(fileStorageService::deleteFile);

        Product finalProduct = productRepository.findByIdWithCategory(productId).orElseThrow();
        Hibernate.initialize(finalProduct.getImages());
        return mapToDTO(finalProduct);
    }

    private void mapDtoToProductEntity(ProductRequestDTO dto, Product product) {
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock() != null ? dto.getStock() : 0);
        product.setBarcode(dto.getBarcode());
        product.setBrand(dto.getBrand());
        product.setModel(dto.getModel());
        product.setMovement(dto.getMovement());
        product.setCaseMaterial(dto.getCaseMaterial());
        product.setStrapMaterial(dto.getStrapMaterial());
        product.setDialColor(dto.getDialColor());
        product.setWaterResistance(dto.getWaterResistance());
    }

    private void updateProductImages(Product product, List<String> newImageUrls, String newPrimaryImageUrl) {
        List<ProductImage> imagesToRemove = new ArrayList<>(product.getImages());
        product.getImages().clear();

        if (!imagesToRemove.isEmpty()) {
            productImageRepository.deleteAllInBatch(imagesToRemove);
        }

        if (!CollectionUtils.isEmpty(newImageUrls)) {
            int displayOrder = 0;
            for (String url : newImageUrls) {
                if (url == null || url.trim().isEmpty()) continue;
                ProductImage productImage = new ProductImage();
                productImage.setProduct(product);
                productImage.setImageUrl(url.trim());
                productImage.setPrimary(url.trim().equals(newPrimaryImageUrl));
                productImage.setDisplayOrder(displayOrder++);
                product.getImages().add(productImage);
            }
        }
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<Product> findAllProductsEntity() {
        List<Product> products = productRepository.findAll();
        products.forEach(p -> Hibernate.initialize(p.getImages()));
        return products;
    }

    @Transactional(readOnly = true)
    public Optional<Product> findProductByNameForDialogflow(String productName) {
        if (productName == null || productName.trim().isEmpty()) {
            return Optional.empty();
        }
        Optional<Product> productOpt = productRepository.findFirstByNameContainingIgnoreCase(productName.trim());
        productOpt.ifPresent(p -> Hibernate.initialize(p.getImages()));
        return productOpt;
    }

    @Transactional
    public ProductDTO save(Product productInput) {
        if (productInput.getCategory() == null || productInput.getCategory().getCategoryId() <= 0) {
            throw new BadRequestException("Category ID is required when creating a product.");
        }
        Category category = categoryRepository.findById(productInput.getCategory().getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", productInput.getCategory().getCategoryId()));
        productInput.setCategory(category); // Set category được quản lý

        // TODO: Xử lý lưu danh sách ảnh
        if (productInput.getImages() != null) {
            productInput.getImages().forEach(img -> {
                img.setProduct(productInput); // Gắn quan hệ 2 chiều
            });
        }

        Product savedProduct = productRepository.save(productInput);
        Hibernate.initialize(savedProduct.getCategory());
        Hibernate.initialize(savedProduct.getImages());
        return mapToDTO(savedProduct);
    }


    @Transactional
    public ProductDTO update(int id, Product productDetails) {
        Product product = findProductEntityById(id); // Lấy entity hiện có

        // Validate và cập nhật Category
        if (productDetails.getCategory() == null || productDetails.getCategory().getCategoryId() <= 0) {
            throw new BadRequestException("Category ID is required for update.");
        }
        if (product.getCategory() == null || product.getCategory().getCategoryId() != productDetails.getCategory().getCategoryId()) {
            Category category = categoryRepository.findById(productDetails.getCategory().getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", productDetails.getCategory().getCategoryId()));
            product.setCategory(category);
        }

        // Cập nhật các trường khác
        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setStock(productDetails.getStock());
        product.setBarcode(productDetails.getBarcode());
        product.setBrand(productDetails.getBrand());
        product.setModel(productDetails.getModel());
        product.setMovement(productDetails.getMovement());
        product.setCaseMaterial(productDetails.getCaseMaterial());
        product.setStrapMaterial(productDetails.getStrapMaterial());
        product.setDialColor(productDetails.getDialColor());
        product.setWaterResistance(productDetails.getWaterResistance());

        product.getImages().clear();
        if (productDetails.getImages() != null) {
            productDetails.getImages().forEach(img -> {
                img.setProduct(product); // Quan trọng: set lại quan hệ
                product.getImages().add(img);
            });
        }

        Product updatedProduct = productRepository.save(product);
        Hibernate.initialize(updatedProduct.getCategory()); // Đảm bảo load để map DTO
        Hibernate.initialize(updatedProduct.getImages());   // Đảm bảo load để map DTO
        return mapToDTO(updatedProduct);
    }

    //    @Transactional
//    public void delete(int id) {
//        Product product = productRepository.findByIdWithCategory(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", id));
//        Hibernate.initialize(product.getImages());
//
//        if (product.getImages() != null && !product.getImages().isEmpty()) {
//            product.getImages().forEach(image -> fileStorageService.deleteFile(image.getImageUrl()));
//        }
//        productRepository.delete(product);
//    }

    @Transactional
    public ProductDTO toggleProductVisibility(int productId) {
        Product product = findProductEntityById(productId);
        product.setVisible(!product.isVisible()); // Đảo ngược trạng thái visible
        // productRepository.save(product); // Không cần thiết nếu findProductEntityById trả về managed entity và method này là @Transactional
        logger.info("Product ID {} visibility toggled to: {}", productId, product.isVisible());
        // Cần cập nhật lại ProductReviewService.updateProductAverageRatingAndCount nếu việc ẩn sản phẩm cũng nên ẩn review của nó khỏi tính toán
        // Hoặc giữ nguyên, tùy logic bạn muốn. Hiện tại updateProductAverageRatingAndCount đã lọc theo review.visible
        return mapToDTO(product);
    }


    @Transactional
    public void decreaseStock(int productId, int quantity) {
        int updatedRows = productRepository.decreaseStock(productId, quantity);
        if (updatedRows == 0) {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId + " for stock check."));
            throw new OperationFailedException(String.format("Failed to decrease stock for product %s (ID: %d). Available: %d, Requested: %d",
                    product.getName(), productId, product.getStock(), quantity));
        }
    }

    @Transactional
    public void increaseStock(int productId, int quantity) {
        int updatedRows = productRepository.increaseStock(productId, quantity);
        if (updatedRows == 0) {
            throw new OperationFailedException(String.format("Failed to increase stock for product %d. Product might not exist.", productId));
        }
    }

    @Transactional(readOnly = true)
    public Map<Integer, Product> findProductsMapByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Product> products = productRepository.findAllByIdWithCategory(ids);
        products.forEach(p -> Hibernate.initialize(p.getImages()));
        return products.stream().collect(Collectors.toMap(Product::getProductId, Function.identity()));
    }

    @Transactional(readOnly = true)
    public Page<ProductDTO> findVisibleProductsByFilter(String name, Integer categoryId,
                                                        BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        BigDecimal min = (minPrice == null || minPrice.compareTo(BigDecimal.ZERO) < 0) ? BigDecimal.ZERO : minPrice;
        BigDecimal max = (maxPrice == null || maxPrice.compareTo(BigDecimal.ZERO) < 0) ? new BigDecimal("9999999999.99") : maxPrice;
        String searchName = (name == null ? "" : name.trim().toLowerCase());

        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("createdAt").descending());
            // Hoặc "updatedAt"
        }

        Page<Product> productPage;
        if (categoryId == null) {
            productPage = productRepository.findVisibleByNameContainingIgnoreCaseAndPriceBetween(searchName, min, max, pageable);
        } else {
            if (!categoryRepository.existsById(categoryId)) {
                throw new ResourceNotFoundException("Category", "categoryId", categoryId);
            }
            productPage = productRepository.findVisibleByNameContainingIgnoreCaseAndCategoryCategoryIdAndPriceBetween(searchName, categoryId, min, max, pageable);
        }
        return mapPageToDTO(productPage);
    }

    // Hàm này dùng cho ADMIN (lấy tất cả sản phẩm, bao gồm cả visible và hidden)
    @Transactional(readOnly = true)
    public Page<ProductDTO> findAllProductsByFilterForAdmin(String name, Integer categoryId,
                                                            BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        BigDecimal min = (minPrice == null || minPrice.compareTo(BigDecimal.ZERO) < 0) ? BigDecimal.ZERO : minPrice;
        BigDecimal max = (maxPrice == null || maxPrice.compareTo(BigDecimal.ZERO) < 0) ? new BigDecimal("9999999999.99") : maxPrice;
        String searchName = (name == null ? "" : name.trim().toLowerCase());

        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("createdAt").descending());
        }

        Page<Product> productPage;
        if (categoryId == null) {
            // Sử dụng query đã đổi tên trong repository để lấy tất cả (không lọc visible)
            productPage = productRepository.findAllByNameContainingIgnoreCaseAndPriceBetweenForAdmin(searchName, min, max, pageable);
        } else {
            if (!categoryRepository.existsById(categoryId)) {
                throw new ResourceNotFoundException("Category", "categoryId", categoryId);
            }
            productPage = productRepository.findAllByNameContainingIgnoreCaseAndCategoryCategoryIdAndPriceBetweenForAdmin(searchName, categoryId, min, max, pageable);
        }
        return mapPageToDTO(productPage);
    }


}