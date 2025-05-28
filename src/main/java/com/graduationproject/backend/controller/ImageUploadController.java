//package com.graduationproject.backend.controller;
//
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.UUID;
//
//@RestController
//@RequestMapping("/api")
//public class ImageUploadController {
//
//    @PostMapping("/upload-image")
//    public ResponseEntity<String> uploadImage(@RequestParam("file") MultipartFile file) {
//        try {
//            String uploadDir = "uploads/images/";
//            File dir = new File(uploadDir);
//            if (!dir.exists()) dir.mkdirs();
//
//            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
//            Path filePath = Paths.get(uploadDir, filename);
//            Files.write(filePath, file.getBytes());
//
//            String imageUrl = "/images/" + filename;
//            return ResponseEntity.ok(imageUrl);
//        } catch (IOException e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Upload failed");
//        }
//    }
//}
