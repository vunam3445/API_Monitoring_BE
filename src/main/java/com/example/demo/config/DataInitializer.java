package com.example.demo.config;

import com.example.demo.entities.User;
import com.example.demo.enums.UserRole;
import com.example.demo.enums.UserStatus;
import com.example.demo.repositories.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        return args -> {
            String adminEmail = "admin@gmail.com";

            // 1. Kiểm tra xem Admin đã tồn tại chưa để tránh tạo trùng mỗi khi restart app
            if (userRepository.findByEmail(adminEmail).isEmpty()) {
                User admin = new User();
                admin.setEmail(adminEmail);
                // Bạn nên đổi mật khẩu này ngay sau khi đăng nhập lần đầu
                admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
                admin.setFullName("System Administrator");
                admin.setRole(UserRole.ADMIN); // Role quyền lực nhất
                admin.setStatus(UserStatus.ACTIVE);
                admin.setProvider("local");
                admin.setPlanType("ENTERPRISE"); // Admin mặc định dùng gói cao nhất
                admin.setCreatedAt(LocalDateTime.now());

                userRepository.save(admin);
                System.out.println(">>> Default Admin created: " + adminEmail);
            } else {
                System.out.println(">>> Default Admin already exists. Skipping...");
            }
        };
    }
}