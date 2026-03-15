package com.example.demo.config;

import com.example.demo.entities.*;
import com.example.demo.enums.*;
import com.example.demo.repositories.subscriptionPlan.SubscriptionPlanRepository;
import com.example.demo.repositories.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Configuration
public class DataInitializer {

    @Bean
    @Transactional
    CommandLineRunner initDatabase(
            UserRepository userRepository,
            SubscriptionPlanRepository planRepository,
            BCryptPasswordEncoder passwordEncoder) {
        return args -> {
            // 1. Chỉ tạo các gói cước để hệ thống có sẵn danh mục gói
            createPlanIfNotExist(planRepository, "FREE", 0.0, 5, 300, "{\"ads\": true}");
            createPlanIfNotExist(planRepository, "PRO", 19.9, 50, 60, "{\"custom_reports\": true}");
            createPlanIfNotExist(planRepository, "ENTERPRISE", 99.0, 1000, 30, "{\"api_access\": true}");

            // 2. Tạo Admin tối giản
            String adminEmail = "admin@gmail.com";
            if (userRepository.findByEmail(adminEmail).isEmpty()) {
                User admin = new User();
                admin.setEmail(adminEmail);
                admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
                admin.setFullName("System Administrator");
                admin.setRole(UserRole.ADMIN);
                admin.setStatus(UserStatus.ACTIVE);
                admin.setProvider("local");

                // planType để null (không set)
                admin.setPlanType(null);

                admin.setCreatedAt(LocalDateTime.now());

                userRepository.save(admin);
                System.out.println(">>> Created Admin User with no planType and no subscription.");
            }
        };
    }

    private void createPlanIfNotExist(SubscriptionPlanRepository repo, String name, Double price, Integer maxMonitors, Integer minInterval, String features) {
        if (repo.findByName(name).isEmpty()) {
            SubscriptionPlan plan = new SubscriptionPlan();
            plan.setName(name);
            plan.setPrice(price);
            plan.setMaxMonitors(maxMonitors);
            plan.setMinInterval(minInterval);
            plan.setFeatures(features);
            plan.setIsActive(true);
            repo.save(plan);
            System.out.println(">>> Created Plan: " + name);
        }
    }
}