package com.graduationproject.backend.exception;

public class ResourceNotFoundException extends RuntimeException {
    private String messageKey;
    private Object[] args;

    // Constructor cũ có thể giữ lại để tương thích
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s : '%s'", resourceName, fieldName, fieldValue));
        // Đặt một key chung hoặc logic để tạo key dựa trên resourceName, fieldName
        this.messageKey = "error.resource.notFound.detail"; // Ví dụ: messages.properties có error.resource.notFound.detail = {0} không tìm thấy với {1} : ''{2}''
        this.args = new Object[]{resourceName, fieldName, String.valueOf(fieldValue)};
    }

    // Constructor mới với key và args
    public ResourceNotFoundException(String messageKey, Object... args) {
        super(messageKey); // Message của super có thể là key nếu không tìm thấy bản dịch
        this.messageKey = messageKey;
        this.args = args;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getArgs() {
        return args;
    }
}