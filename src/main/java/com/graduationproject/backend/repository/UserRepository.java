package com.graduationproject.backend.repository;

import com.graduationproject.backend.entity.User;
import com.graduationproject.backend.entity.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

import java.sql.Timestamp;
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findById(long id);

    @Query("SELECT u FROM User u WHERE u.role = :role AND (CONCAT(u.firstName, ' ', u.lastName) LIKE %:keyword% OR u.email LIKE %:keyword%)")
    Page<User> searchByRoleAndNameOrEmail(@Param("role") Role role, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT u FROM User u WHERE LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<User> searchByNameOrEmail(@Param("search") String search, Pageable pageable);
    Page<User> findByRole(Role role, Pageable pageable);

    boolean existsByUsername(String username);



    //new update
    // Thống kê tổng số người dùng mới trong một khoảng thời gian
    long countByCreatedAtBetween(Timestamp startDate, Timestamp endDate);

    // Tổng số người dùng (cho dashboard)
    long count();
    long countByRole(Role role);

}