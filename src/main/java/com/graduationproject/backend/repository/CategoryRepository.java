package com.graduationproject.backend.repository;

import com.graduationproject.backend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    List<Category> findByVisibleTrueOrderByNameAsc();

    List<Category> findByVisibleTrueOrderByCreatedAtDesc();
}
