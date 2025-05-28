package com.graduationproject.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger; // Thêm Logger
import org.slf4j.LoggerFactory; // Thêm Logger

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class); // Thêm Logger
    private final Path fileStorageLocation;
    private final String fileStorageBaseUrl;

    public FileStorageService(@Value("${file.upload-dir}") String uploadDir,
                              @Value("${file.base-url}") String baseUrl) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.fileStorageBaseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";

        try {
            Files.createDirectories(this.fileStorageLocation);
            logger.info("Created upload directory: {}", this.fileStorageLocation.toString());
        } catch (Exception ex) {
            logger.error("Could not create the directory where the uploaded files will be stored.", ex);
            throw new RuntimeException("Could not create the directory for uploaded files.", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("Failed to store empty file.");
        }
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = "";
        try {
            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf(".")).toLowerCase();
            }
            // Kiểm tra extension hợp lệ nếu cần
            // if (!Arrays.asList(".jpg", ".jpeg", ".png", ".gif").contains(fileExtension)) {
            //     throw new RuntimeException("Invalid file type: " + fileExtension);
            // }

            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

            if (uniqueFileName.contains("..")) {
                throw new RuntimeException("Filename contains invalid path sequence: " + uniqueFileName);
            }

            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Stored file: {}", targetLocation.toString());
            }
            return this.fileStorageBaseUrl + uniqueFileName;
        } catch (IOException ex) {
            logger.error("Could not store file {}. Please try again!", originalFileName, ex);
            throw new RuntimeException("Could not store file " + originalFileName + ". Please try again!", ex);
        }
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.trim().isEmpty() || !fileUrl.startsWith(this.fileStorageBaseUrl)) {
            logger.warn("Attempted to delete invalid or unmanaged file URL: {}", fileUrl);
            return;
        }
        try {
            String fileName = fileUrl.substring(this.fileStorageBaseUrl.length());
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                logger.info("Deleted file: {}", filePath.toString());
            } else {
                logger.warn("File not found for deletion: {}", filePath.toString());
            }
        } catch (IOException ex) {
            logger.error("Could not delete file: {}. Error: {}", fileUrl, ex.getMessage());
            // Không ném lỗi ở đây để không làm gián đoạn các thao tác khác, chỉ log lỗi
        }
    }
}