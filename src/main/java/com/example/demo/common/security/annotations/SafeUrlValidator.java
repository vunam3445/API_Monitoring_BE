package com.example.demo.common.security.annotations;

import com.example.demo.common.exceptions.UrlSecurityValidationException;
import com.example.demo.common.security.UrlSecurityValidator;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Thực thi logic kiểm tra bảo mật của annotation @SafeUrl.
 * Gọi vào UrlSecurityValidator và bắt UrlSecurityValidationException
 * để gán thông báo lỗi chi tiết vào ConstraintValidatorContext.
 */
@Component
public class SafeUrlValidator implements ConstraintValidator<SafeUrl, String> {

    @Autowired
    private UrlSecurityValidator urlSecurityValidator;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            // @NotBlank sẽ bắt trường hợp này, không cần lặp lại
            return true;
        }

        try {
            urlSecurityValidator.validateUrl(value);
            return true;
        } catch (UrlSecurityValidationException e) {
            // Gán thông báo lỗi chi tiết từ validator vào context
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(e.getMessage())
                    .addConstraintViolation();
            return false;
        }
    }
}
