package com.graduationproject.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Dùng cho các lỗi logic nghiệp vụ không mong muốn
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class OperationFailedException extends RuntimeException {
    public OperationFailedException(String message) {
        super(message);
    }

     public OperationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}