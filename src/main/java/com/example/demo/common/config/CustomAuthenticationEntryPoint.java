package com.example.demo.common.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // Trả về 401
        response.setContentType("application/json;charset=UTF-8");

        // Bạn có thể viết JSON chi tiết hơn ở đây
        String jsonResponse = String.format(
                "{\"status\": 401, \"error\": \"Unauthorized\", \"message\": \"%s\"}",
                "Token hết hạn hoặc không hợp lệ"
        );
        System.out.println("Lỗi xác thực: " + authException.getMessage());
        response.getWriter().write(jsonResponse);
    }
}
