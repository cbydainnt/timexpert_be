package com.graduationproject.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder; // Để lấy Locale hiện tại
import org.springframework.stereotype.Service;
import java.util.Locale;

@Service
public class MyExampleService {

    @Autowired
    private MessageSource messageSource;

    public String getGreetingMessage() {
        Locale currentLocale = LocaleContextHolder.getLocale(); // Lấy Locale hiện tại của request
        return messageSource.getMessage("greeting.welcome", null, currentLocale);
        // Tham số thứ 2 là mảng các đối số nếu message của bạn có placeholder (ví dụ: "Chào {0}")
        // Tham số thứ 3 là Locale mong muốn
    }

    public String getErrorMessageUserNotFound() {
        return messageSource.getMessage("error.user.notFound", null, LocaleContextHolder.getLocale());
    }
}