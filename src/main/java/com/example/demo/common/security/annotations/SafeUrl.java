package com.example.demo.common.security.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom Jakarta Validation annotation để kiểm tra URL an toàn.
 * Thay thế @URL của Hibernate để bổ sung kiểm tra bảo mật SSRF.
 *
 * Sử dụng: đặt @SafeUrl lên field url trong DTO thay vì @URL.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = SafeUrlValidator.class)
public @interface SafeUrl {
    String message() default "URL không an toàn hoặc trỏ tới địa chỉ mạng nội bộ";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
