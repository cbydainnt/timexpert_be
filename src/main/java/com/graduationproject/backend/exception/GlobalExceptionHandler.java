package com.graduationproject.backend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Autowired
    private MessageSource messageSource; // Inject MessageSource

    private String getMessage(String code, Object... args) {
        Locale currentLocale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, args, code, currentLocale); // Trả về code nếu không tìm thấy message
    }


    // Xử lý lỗi ResourceNotFoundException
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorDetails> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(new Date(), ex.getMessage(), request.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
    }

    // Xử lý lỗi BadRequestException
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorDetails> handleBadRequestException(BadRequestException ex, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(new Date(), ex.getMessage(), request.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

     // Xử lý lỗi OperationFailedException
    @ExceptionHandler(OperationFailedException.class)
    public ResponseEntity<ErrorDetails> handleOperationFailedException(OperationFailedException ex, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(new Date(), ex.getMessage(), request.getDescription(false));
        // Thường trả về 500, nhưng có thể tùy chỉnh nếu cần
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }


    // Xử lý lỗi validation (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        // Trả về lỗi 400 Bad Request cho lỗi validation
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    // Xử lý các lỗi chung khác (nên là lỗi cuối cùng)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetails> handleGlobalException(Exception ex, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(new Date(), "An internal server error occurred", request.getDescription(false));
        // Log lỗi gốc ra console/file để debug
        ex.printStackTrace(); // Quan trọng: Để lại dấu vết lỗi gốc
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Lớp helper để chứa chi tiết lỗi trả về
    public static class ErrorDetails {
        private Date timestamp;
        private String message;
        private String details;

        public ErrorDetails(Date timestamp, String message, String details) {
            super();
            this.timestamp = timestamp;
            this.message = message;
            this.details = details;
        }

        // Getters
        public Date getTimestamp() { return timestamp; }
        public String getMessage() { return message; }
        public String getDetails() { return details; }
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorDetails> handleNoResourceFoundException(NoResourceFoundException ex, WebRequest request) {
        logger.warn("No resource found: {}", request.getDescription(false)); // Thêm logger nếu chưa có
        ErrorDetails errorDetails = new ErrorDetails(new Date(), "Không tìm thấy tài nguyên yêu cầu.", request.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
    }

}