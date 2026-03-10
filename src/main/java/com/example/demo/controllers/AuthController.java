package com.example.demo.controllers;


import com.example.demo.dto.request.GoogleLoginRequest;
import com.example.demo.dto.request.LoginRequest;
import com.example.demo.dto.request.RegisterRequest;
import com.example.demo.dto.response.LoginResponse;
import com.example.demo.entities.User;
import com.example.demo.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.Cookie;
import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request);

        // Không nên trả về object User (vì có password_hash)
        // Trả về một message hoặc ID là đủ
        return ResponseEntity.ok("Đăng ký thành công cho email: " + user.getEmail());
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        LoginResponse loginData = authService.login(request);

        // Tạo Cookie chứa Refresh Token
        ResponseCookie cookie = ResponseCookie.from("refreshToken", loginData.getRefreshToken())
                .httpOnly(true)    // Quan trọng: JavaScript không thể đọc được, chống XSS
                .secure(false)     // Để false nếu đang chạy http://localhost, true nếu chạy https
                .path("/")
                .maxAge(7 * 24 * 60 * 60) // 7 ngày
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // Chỉ trả AccessToken về Body để React dùng
        loginData.setRefreshToken(null);
        return ResponseEntity.ok(loginData);
    }
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        String refreshToken = null;

        // Lấy token từ Cookie
        if (request.getCookies() != null) {
            refreshToken = Arrays.stream(request.getCookies())
                    .filter(c -> "refreshToken".equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing Refresh Token");
        }

        try {
            String newAccessToken = authService.refreshAccessToken(refreshToken);
            return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @PostMapping("/google")
    public ResponseEntity<LoginResponse> googleLogin(@RequestBody GoogleLoginRequest request, HttpServletResponse response) {
        LoginResponse loginData = authService.loginGoogle(request);

        // Đừng quên đính kèm Refresh Token vào Cookie như bạn đã làm ở hàm Login thường
        ResponseCookie cookie = ResponseCookie.from("refreshToken", loginData.getRefreshToken())
                .httpOnly(true)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        loginData.setRefreshToken(null);
        return ResponseEntity.ok(loginData);
    }
}