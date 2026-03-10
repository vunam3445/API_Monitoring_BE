package com.example.demo.services;

import com.example.demo.dto.request.GoogleLoginRequest;
import com.example.demo.dto.request.LoginRequest;
import com.example.demo.dto.request.RegisterRequest;
import com.example.demo.dto.response.LoginResponse;
import com.example.demo.entities.User;
import com.example.demo.enums.UserRole;
import com.example.demo.enums.UserStatus;
import com.example.demo.repositories.user.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${google.client-id}")
    private String googleClientId;

    public AuthService(UserRepository userRepository,
                       BCryptPasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email này đã được đăng ký trong hệ thống.");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getName());
        user.setProvider("local");

        // Cập nhật theo DB mới: Sử dụng Enum
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setPlanType("FREE"); // Có thể gán từ hằng số cấu hình hệ thống
        user.setCreatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email hoặc mật khẩu không chính xác"));

        // Kiểm tra trạng thái tài khoản (DB mới có status)
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new RuntimeException("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ hỗ trợ.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Email hoặc mật khẩu không chính xác");
        }

        return generateLoginResponse(user);
    }

    @Transactional
    public LoginResponse loginGoogle(GoogleLoginRequest request) {
        GoogleIdToken.Payload payload = verifyGoogleToken(request.getCredential());
        String email = payload.getEmail();

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            // Tạo user mới nếu chưa tồn tại
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setRole(UserRole.USER);
            newUser.setStatus(UserStatus.ACTIVE);
            newUser.setPlanType("FREE");
            return newUser;
        });

        // Kiểm tra status nếu là user cũ
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new RuntimeException("Tài khoản liên kết Google của bạn đã bị khóa.");
        }

        // Cập nhật thông tin từ Google
        user.setFullName((String) payload.get("name"));
        user.setAvatarUrl((String) payload.get("picture"));
        user.setProvider("google");
        user.setProviderId(payload.getSubject()); // Subject là ID duy nhất của Google

        return generateLoginResponse(user);
    }

    /**
     * Logic dùng chung để tạo Token, cập nhật thời gian đăng nhập và trả về Response
     */
    private LoginResponse generateLoginResponse(User user) {
        String accessToken = jwtService.generateToken(user);
        String refreshToken = UUID.randomUUID().toString();

        // Cập nhật thông tin phiên và login (DB mới)
        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiry(LocalDateTime.now().plusDays(7));
        user.setLastLoginAt(LocalDateTime.now()); // Trường mới trong DB

        userRepository.save(user);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .planType(user.getPlanType())
                .role(user.getRole().name()) // Trả về role để FE phân quyền UI
                .build();
    }

    @Transactional
    public String refreshAccessToken(String refreshToken) {
        User user = userRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh Token không hợp lệ"));

        if (user.getRefreshTokenExpiry().isBefore(LocalDateTime.now())) {
            user.setRefreshToken(null);
            userRepository.save(user);
            throw new RuntimeException("Phiên đăng nhập hết hạn, vui lòng đăng nhập lại");
        }

        // Vẫn check status khi refresh token để đảm bảo nếu vừa bị khóa thì ko dùng tiếp được
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new RuntimeException("Tài khoản đã bị khóa.");
        }

        return jwtService.generateToken(user);
    }

    private GoogleIdToken.Payload verifyGoogleToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                return idToken.getPayload();
            } else {
                throw new RuntimeException("Google Token không hợp lệ");
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xác thực Google Token: " + e.getMessage());
        }
    }
}