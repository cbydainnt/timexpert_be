package com.graduationproject.backend.service;

import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.graduationproject.backend.exception.OperationFailedException; // Import
import org.slf4j.Logger; // Import
import org.slf4j.LoggerFactory; // Import
import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException; // Import

@Service
public class BarcodeService {

     private static final Logger logger = LoggerFactory.getLogger(BarcodeService.class);

    public String decodeBarcode(File imageFile) {
        BufferedImage bufferedImage;
        try {
            bufferedImage = ImageIO.read(imageFile);
            if (bufferedImage == null) {
                 throw new OperationFailedException("Could not read image file or file is not an image: " + imageFile.getName());
            }
        } catch (IOException e) {
             logger.error("IOException reading barcode image file: {}", imageFile.getPath(), e);
             throw new OperationFailedException("Error reading barcode image file.", e);
        }

        LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        try {
            Result result = new MultiFormatReader().decode(bitmap);
            if (result != null) {
                return result.getText();
            } else {
                 // Trường hợp decode thành công nhưng không có text (hiếm)
                 throw new OperationFailedException("Barcode decoded successfully but contained no text.");
            }
        } catch (NotFoundException e) {
             // Đây là lỗi thường gặp nhất: không tìm thấy barcode trong ảnh
             logger.warn("Barcode not found in image: {}", imageFile.getName());
             throw new OperationFailedException("No barcode found in the provided image.", e);
//          catch   (ChecksumException | FormatException e)
        } catch (OperationFailedException e) {
             // Lỗi định dạng hoặc checksum của barcode
             logger.error("Error decoding barcode due to format/checksum error in image: {}", imageFile.getName(), e);
              throw new OperationFailedException("Could not decode barcode due to format or checksum error.", e);
        } catch (Exception e) { // Bắt các lỗi khác của thư viện zxing
             logger.error("Unexpected error decoding barcode from image: {}", imageFile.getName(), e);
             throw new OperationFailedException("An unexpected error occurred during barcode decoding.", e);
        }
    }
}