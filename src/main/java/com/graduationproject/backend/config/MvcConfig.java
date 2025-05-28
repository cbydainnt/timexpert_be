package com.graduationproject.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Paths; // Thêm import này

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.base-url}")
    private String baseUrl; // Ví dụ: /uploads/images/

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String resolvedBaseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        // Quan trọng: Paths.get(uploadDir).toAbsolutePath().toString() sẽ trả về đường dẫn tuyệt đối.
        // Đảm bảo rằng thư mục này tồn tại và ứng dụng có quyền ghi vào đó.
        String absoluteUploadDir = Paths.get(uploadDir).toAbsolutePath().normalize().toString();

        // Thêm dấu / vào cuối nếu nó là thư mục
        if (!absoluteUploadDir.endsWith(System.getProperty("file.separator"))) {
            absoluteUploadDir += System.getProperty("file.separator");
        }

        System.out.println("Serving static files from URL: " + resolvedBaseUrl + "**");
        System.out.println("Mapped to physical path: " + "file:" + absoluteUploadDir);


        registry.addResourceHandler(resolvedBaseUrl + "**")
                .addResourceLocations("file:" + absoluteUploadDir);
    }
}