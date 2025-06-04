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

//    @GetMapping
//    public ResponseEntity<List<CategoryDTO>> getAllCategories() {
//        List<CategoryDTO> categoryDTOs = categoryService.getAllCategories();
//        return ResponseEntity.ok(categoryDTOs);
//    }

    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getAllVisibleCategories() {
        List<CategoryDTO> categoryDTOs = categoryService.getAllVisibleCategories();
        return ResponseEntity.ok(categoryDTOs);
    }

//    @GetMapping("/{id}")
//    public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable int id) {
//        CategoryDTO categoryDTO = categoryService.getCategoryById(id);
//         // Service ném lỗi nếu không tìm thấy
//        return ResponseEntity.ok(categoryDTO);
//    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable int id) {
        CategoryDTO categoryDTO = categoryService.getCategoryById(id);
        // Nếu là user, chỉ cho xem nếu category visible
        // Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // boolean isAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        // if (!categoryDTO.isVisible() && !isAdmin) {
        //     throw new ResourceNotFoundException("Category", "id", id);
        // }
        return ResponseEntity.ok(categoryDTO);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CategoryDTO>> getAllCategoriesForAdmin() {
        List<CategoryDTO> categoryDTOs = categoryService.getAllCategoriesForAdmin();
        return ResponseEntity.ok(categoryDTOs);
    }


    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryDTO> createCategory(@Valid @RequestBody CategoryDTO categoryDTO) {
        CategoryDTO createdCategoryDTO = categoryService.createCategory(categoryDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCategoryDTO);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
     // Nhận vào DTO và dùng @Valid
    public ResponseEntity<CategoryDTO> updateCategory(@PathVariable int id, @Valid @RequestBody CategoryDTO categoryDetails) {
        CategoryDTO updatedCategoryDTO = categoryService.updateCategory(id, categoryDetails);
        return ResponseEntity.ok(updatedCategoryDTO);
    }

    @PutMapping("/admin/toggle-visibility/{id}") // Endpoint mới cho Admin ẩn/hiện
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryDTO> toggleCategoryVisibility(@PathVariable int id) {
        CategoryDTO updatedCategory = categoryService.toggleCategoryVisibility(id);
        return ResponseEntity.ok(updatedCategory);
    }

//    @DeleteMapping("/{id}")
//    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
//    public ResponseEntity<Void> deleteCategory(@PathVariable int id) {
//        categoryService.deleteCategory(id);
//         // Service ném lỗi nếu không tìm thấy hoặc không thể xóa
//        return ResponseEntity.noContent().build();
//    }
}