package com.graduationproject.backend.service;

import com.graduationproject.backend.dto.FavoriteDTO;
import com.graduationproject.backend.dto.ProductDTO; // Import ProductDTO
import com.graduationproject.backend.entity.Favorite;
import com.graduationproject.backend.entity.Product;
import com.graduationproject.backend.entity.User;
import com.graduationproject.backend.exception.BadRequestException;
import com.graduationproject.backend.exception.ResourceNotFoundException;
import com.graduationproject.backend.repository.FavoriteRepository;
import com.graduationproject.backend.repository.UserRepository;
// Import ProductService thay vì ProductRepository
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import Transactional

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors; // Import Collectors

@Service
public class FavoriteService {

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private UserRepository userRepository;

    // Inject ProductService để lấy ProductDTO và kiểm tra tồn tại
    @Autowired
    private ProductService productService;

    // Inject UserService để lấy User entity
    @Autowired
    private UserService userService;

     // Helper map Favorite Entity sang DTO
     private FavoriteDTO mapToDTO(Favorite favorite) {
         if (favorite == null) return null;
         FavoriteDTO dto = new FavoriteDTO();
         dto.setId(favorite.getId());
         dto.setCreatedAt(favorite.getCreatedAt());
         if (favorite.getUser() != null) {
             dto.setUserId(favorite.getUser().getUserId());
         }
         if (favorite.getProduct() != null) {
             // Lấy ProductDTO từ ProductService
             dto.setProduct(productService.mapToDTO(favorite.getProduct()));
         }
         return dto;
     }


    @Transactional
    public FavoriteDTO addFavorite(long userId, int productId) {
        // Kiểm tra xem đã favorite chưa
        Optional<Favorite> existing = favoriteRepository.findByUserUserIdAndProductProductId(userId, productId);
        if (existing.isPresent()) {
             // Nếu đã tồn tại, trả về thông tin hiện có dưới dạng DTO
             return mapToDTO(existing.get());
            // Hoặc có thể ném lỗi BadRequestException("Product already favorited");
        }

        // Lấy User entity
         User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

        // Kiểm tra Product tồn tại (thông qua ProductService)
        Product product = productService.findProductEntityById(productId);

        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setProduct(product);
        Favorite savedFavorite = favoriteRepository.save(favorite);
        return mapToDTO(savedFavorite); // Trả về DTO
    }

//    @Transactional(readOnly = true)
//    public List<FavoriteDTO> getFavorites(long userId) {
//        // Kiểm tra user tồn tại
//         if (!userRepository.existsById(userId)) {
//              throw new ResourceNotFoundException("User", "userId", userId);
//         }
//        List<Favorite> favorites = favoriteRepository.findByUserUserId(userId);
//        // Map list Entity sang list DTO
//        return favorites.stream().map(this::mapToDTO).collect(Collectors.toList());
//    }
    @Transactional(readOnly = true)
    public List<FavoriteDTO> getFavorites(long userId) {
        List<Favorite> favs = favoriteRepository.findByUserWithProductAndCategory(userId);
        return favs.stream().map(this::mapToDTO).collect(Collectors.toList());
    }
//    @Transactional(readOnly = true)
//    public List<FavoriteDTO> getFavorites(long userId) {
//        if (!userRepository.existsById(userId)) {
//              throw new ResourceNotFoundException("User", "userId", userId);
//         }
//        List<Favorite> favorites = favoriteRepository.findByUserWithProduct(userId);
//        return favorites.stream()
//                .map(this::mapToDTO)
//                .collect(Collectors.toList());
//    }

    @Transactional
    public void removeFavorite(long userId, long favoriteId) {
         // Kiểm tra xem Favorite có tồn tại và thuộc về đúng user không
         Favorite favorite = favoriteRepository.findById(favoriteId)
                 .orElseThrow(() -> new ResourceNotFoundException("Favorite", "favoriteId", favoriteId));

         if (favorite.getUser() == null || favorite.getUser().getUserId() != userId) {
              throw new BadRequestException("Favorite record does not belong to the specified user.");
         }

        favoriteRepository.deleteById(favoriteId);
    }

     // Có thể thêm hàm xóa theo userId và productId
     @Transactional
     public void removeFavoriteByProduct(long userId, int productId) {
          Favorite favorite = favoriteRepository.findByUserUserIdAndProductProductId(userId, productId)
                  .orElseThrow(() -> new ResourceNotFoundException(
                          String.format("Favorite record not found for user %d and product %d", userId, productId)));
          favoriteRepository.delete(favorite);
     }
}