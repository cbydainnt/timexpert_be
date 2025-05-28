package com.graduationproject.backend.repository;

import com.graduationproject.backend.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUserUserId(long userId);
    Optional<Favorite> findByUserUserIdAndProductProductId(long userId, int productId);
    @Query("SELECT f FROM Favorite f JOIN FETCH f.product WHERE f.user.userId = :userId")
    List<Favorite> findByUserWithProduct(@Param("userId") long userId);
    @Query("""
    SELECT f
      FROM Favorite f
      JOIN FETCH f.product p
      JOIN FETCH p.category
     WHERE f.user.userId = :userId
  """)
    List<Favorite> findByUserWithProductAndCategory(@Param("userId") long userId);
}
