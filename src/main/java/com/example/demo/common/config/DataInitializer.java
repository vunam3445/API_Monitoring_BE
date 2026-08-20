package com.example.demo.common.config;

import com.example.demo.modules.feature.entities.Feature;
import com.example.demo.modules.feature.repositories.FeatureRepository;
import com.example.demo.modules.subscription.entities.SubscriptionPlan;
import com.example.demo.modules.subscription.repositories.SubscriptionPlanRepository;
import com.example.demo.modules.user.entities.User;
import com.example.demo.modules.user.enums.UserRole;
import com.example.demo.modules.user.enums.UserStatus;
import com.example.demo.modules.user.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Configuration
public class DataInitializer {

    @Bean
    @Transactional
    CommandLineRunner initDatabase(
            UserRepository userRepository,
            SubscriptionPlanRepository planRepository,
            FeatureRepository featureRepository,
            BCryptPasswordEncoder passwordEncoder) {
        return args -> {
            // 1. Gieo hạt danh mục các tính năng động (Feature Catalog)
            createFeatureIfNotExist(featureRepository, "5_api_endpoints", "5 API Endpoints Limit", "Cho phép giám sát tối đa 5 API endpoints.");
            createFeatureIfNotExist(featureRepository, "50_api_endpoints", "50 API Endpoints Limit", "Cho phép giám sát tối đa 50 API endpoints.");
            createFeatureIfNotExist(featureRepository, "1000_api_endpoints", "1000 API Endpoints Limit", "Cho phép giám sát tối đa 1000 API endpoints.");
            createFeatureIfNotExist(featureRepository, "email_notifications", "Email Notifications", "Hỗ trợ gửi thông báo cảnh báo qua Email.");
            createFeatureIfNotExist(featureRepository, "slack_notifications", "Slack Integration", "Hỗ trợ gửi thông báo cảnh báo trực tiếp qua kênh Slack.");
            createFeatureIfNotExist(featureRepository, "telegram_notifications", "Telegram Notifications", "Hỗ trợ gửi thông báo cảnh báo qua Telegram.");
            createFeatureIfNotExist(featureRepository, "custom_reports", "Custom Reports", "Hỗ trợ tạo báo cáo tùy chỉnh.");
            createFeatureIfNotExist(featureRepository, "api_access", "API Access", "Cung cấp API truy cập dữ liệu giám sát cho bên thứ ba.");

            // 2. Chỉ tạo các gói cước để hệ thống có sẵn danh mục gói liên kết với Catalog
            createPlanIfNotExist(planRepository, "FREE", 0.0, 5, 300, "{\"5_api_endpoints\": true, \"email_notifications\": true}");
            createPlanIfNotExist(planRepository, "PRO", 19.9, 50, 60, "{\"50_api_endpoints\": true, \"email_notifications\": true, \"slack_notifications\": true, \"custom_reports\": true}");
            createPlanIfNotExist(planRepository, "ENTERPRISE", 99.0, 1000, 30, "{\"1000_api_endpoints\": true, \"email_notifications\": true, \"slack_notifications\": true, \"telegram_notifications\": true, \"custom_reports\": true, \"api_access\": true}");

            // 3. Tạo Admin tối giản
            String adminEmail = "admin@gmail.com";
            if (userRepository.findByEmail(adminEmail).isEmpty()) {
                User admin = new User();
                admin.setEmail(adminEmail);
                admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
                admin.setFullName("System Administrator");
                admin.setRole(UserRole.ADMIN);
                admin.setStatus(UserStatus.ACTIVE);
                admin.setProvider("local");
                admin.setPlanType(null);
                admin.setCreatedAt(LocalDateTime.now());

                userRepository.save(admin);
                System.out.println(">>> Created Admin User with no planType and no subscription.");
            }
        };
    }

    private void createFeatureIfNotExist(FeatureRepository repo, String key, String label, String description) {
        if (!repo.existsByKey(key)) {
            Feature feature = new Feature();
            feature.setKey(key);
            feature.setLabel(label);
            feature.setDescription(description);
            feature.setIsActive(true);
            repo.save(feature);
            System.out.println(">>> Created Feature Catalog: " + key);
        }
    }

    private void createPlanIfNotExist(SubscriptionPlanRepository repo, String name, double price, Integer maxMonitors, Integer minInterval, String features) {
        if (repo.findByName(name).isEmpty()) {
            SubscriptionPlan plan = new SubscriptionPlan();
            plan.setName(name);
            plan.setPrice(BigDecimal.valueOf(price));
            plan.setMaxMonitors(maxMonitors);
            plan.setMinInterval(minInterval);
            plan.setFeatures(features);
            plan.setIsActive(true);
            repo.save(plan);
            System.out.println(">>> Created Plan: " + name);
        }
    }
}
