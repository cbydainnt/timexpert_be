package com.graduationproject.backend.controller;

import com.graduationproject.backend.dto.CategoryDTO; // Import DTO
// import com.graduationproject.backend.entity.Category; // Không cần entity nữa
import com.graduationproject.backend.service.CategoryService;
import jakarta.validation.Valid; // Import Valid
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    @Autowired
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }


    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getAllCategories() {
        List<CategoryDTO> categoryDTOs = categoryService.getAllCategories();
        return ResponseEntity.ok(categoryDTOs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable int id) {
        CategoryDTO categoryDTO = categoryService.getCategoryById(id);
         // Service ném lỗi nếu không tìm thấy
        return ResponseEntity.ok(categoryDTO);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    // Nhận vào DTO và dùng @Valid
    public ResponseEntity<CategoryDTO> createCategory(@Valid @RequestBody CategoryDTO categoryDTO) {
        CategoryDTO createdCategoryDTO = categoryService.createCategory(categoryDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCategoryDTO);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
     // Nhận vào DTO và dùng @Valid
    public ResponseEntity<CategoryDTO> updateCategory(@PathVariable int id, @Valid @RequestBody CategoryDTO categoryDetails) {
        CategoryDTO updatedCategoryDTO = categoryService.updateCategory(id, categoryDetails);
         // Service ném lỗi nếu không tìm thấy
        return ResponseEntity.ok(updatedCategoryDTO);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Void> deleteCategory(@PathVariable int id) {
        categoryService.deleteCategory(id);
         // Service ném lỗi nếu không tìm thấy hoặc không thể xóa
        return ResponseEntity.noContent().build();
    }
}