package com.graduationproject.backend.service;

import com.graduationproject.backend.dto.CategoryDTO; // Import DTO
import com.graduationproject.backend.entity.Category;
import com.graduationproject.backend.exception.ResourceNotFoundException; // Import Exception
import com.graduationproject.backend.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import Transactional

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors; // Import Collectors

@Service
public class CategoryService {

    private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);

    @Autowired
    private CategoryRepository categoryRepository;

    // Helper map Entity sang DTO
    private CategoryDTO mapToDTO(Category category) {
        if (category == null) return null;
        CategoryDTO dto = new CategoryDTO();
        dto.setCategoryId(category.getCategoryId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setVisible(category.isVisible());
        dto.setUpdatedAt(category.getUpdatedAt());
        return dto;
    }

    public List<CategoryDTO> getAllVisibleCategories() {
        List<Category> categories = categoryRepository.findByVisibleTrueOrderByCreatedAtDesc();
        return categories.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public List<CategoryDTO> getAllCategoriesForAdmin() {
        List<Category> categories = categoryRepository.findAll(Sort.by("createdAt").descending());
        return categories.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public List<CategoryDTO> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        return categories.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public CategoryDTO getCategoryById(int id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", id));
        return mapToDTO(category);
    }

     // Lấy Category Entity (dùng nội bộ)
     public Category findCategoryEntityById(int id) {
          return categoryRepository.findById(id)
                 .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", id));
     }


    @Transactional
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        // Có thể kiểm tra tên category đã tồn tại chưa nếu cần
        Category category = new Category();
        category.setName(categoryDTO.getName());
        category.setDescription(categoryDTO.getDescription());
        Category savedCategory = categoryRepository.save(category);
        return mapToDTO(savedCategory);
    }

    @Transactional
    public CategoryDTO updateCategory(int id, CategoryDTO categoryDetails) {
        Category category = findCategoryEntityById(id); // Lấy entity để cập nhật
        category.setName(categoryDetails.getName());
        category.setDescription(categoryDetails.getDescription());
        Category updatedCategory = categoryRepository.save(category);
        return mapToDTO(updatedCategory);
    }

//    @Transactional
//    public void deleteCategory(int id) {
//        // Kiểm tra category có tồn tại không trước khi xóa
//        Category category = findCategoryEntityById(id);
//        // Cần kiểm tra xem có Product nào đang sử dụng Category này không trước khi xóa
//        // Ví dụ: if (productRepository.existsByCategory(category)) { throw new BadRequestException("Cannot delete category with associated products."); }
//        categoryRepository.delete(category);
//    }

    @Transactional
    public CategoryDTO toggleCategoryVisibility(int id) {
        Category category = findCategoryEntityById(id);
        // Kiểm tra xem có Product nào đang sử dụng Category này không TRƯỚC KHI ẨN (nếu logic yêu cầu)
        // if (!category.isVisible() && productRepository.existsByCategoryIdAndVisibleTrue(id)) {
        // throw new BadRequestException("Không thể ẩn danh mục đang có sản phẩm hiển thị.");
        // }
        category.setVisible(!category.isVisible());
        // categoryRepository.save(category); // Không cần nếu managed
        logger.info("Category ID {} visibility toggled to: {}", id, category.isVisible());
        return mapToDTO(category);
    }

}