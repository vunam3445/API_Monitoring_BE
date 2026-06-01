# Dynamic Features Management Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a database-driven dynamic Feature Catalog, a clean RESTful CRUD module for Features, and update the existing Subscription/Plan logic to dynamically resolve features while maintaining 100% backward compatibility and data integrity.

**Architecture:** We use the Hybrid approach (Feature Catalog + JSONB Store). A new `Feature` entity serves as a catalog of all available capabilities. The existing `SubscriptionPlan` maintains its `features` JSONB column (mapping feature keys to boolean status) for maximum performance and backward compatibility. Deletion of a feature is prevented if any plan has it configured as active. The default "FREE" plan name is externalized to application configurations for the Open/Closed Principle.

**Tech Stack:** Java, Spring Boot, Spring Data JPA, MapStruct, Lombok, JUnit, Mockito, Jackson ObjectMapper

---

### File Structure Map
*   **Created Files:**
    *   `src/main/java/com/example/demo/common/exceptions/FeatureInUseException.java` (Custom HTTP 400 Bad Request exception)
    *   `src/main/java/com/example/demo/modules/feature/entities/Feature.java` (JPA Entity for features catalog)
    *   `src/main/java/com/example/demo/modules/feature/repositories/FeatureRepository.java` (JPA Repository for Feature CRUD)
    *   `src/main/java/com/example/demo/modules/feature/dto/CreateFeatureRequest.java` (Create DTO with snake_case validation)
    *   `src/main/java/com/example/demo/modules/feature/dto/UpdateFeatureRequest.java` (Update DTO supporting partial updates, excludes feature key)
    *   `src/main/java/com/example/demo/modules/feature/dto/FeatureResponse.java` (Response DTO)
    *   `src/main/java/com/example/demo/modules/feature/mappers/FeatureMapper.java` (MapStruct mapper extending BaseMapper)
    *   `src/main/java/com/example/demo/modules/feature/services/IFeatureService.java` (Interface defining custom business methods)
    *   `src/main/java/com/example/demo/modules/feature/services/FeatureService.java` (Service extending BaseService with business constraints)
    *   `src/main/java/com/example/demo/modules/feature/controllers/FeatureController.java` (Controller extending BaseController, restricted to Admin)
    *   `src/test/java/com/example/demo/modules/feature/services/FeatureServiceTest.java` (Unit tests for Feature Service)

*   **Modified Files:**
    *   `src/main/resources/application.properties` (Add `app.subscription.default-free-plan-name=FREE`)
    *   `src/main/java/com/example/demo/modules/subscription/services/SubscriptionService.java` (Inject and use `defaultFreePlanName` from config instead of hardcoded "FREE")
    *   `src/main/java/com/example/demo/common/config/DataInitializer.java` (Seed default features first, then map them to default plans dynamically)

---

### Task 1: Create custom exception and config property

**Files:**
- Create: `src/main/java/com/example/demo/common/exceptions/FeatureInUseException.java`
- Modify: `src/main/resources/application.properties`

- [ ] **Step 1: Create FeatureInUseException custom exception**

Create `src/main/java/com/example/demo/common/exceptions/FeatureInUseException.java`:
```java
package com.example.demo.common.exceptions;

import org.springframework.http.HttpStatus;

public class FeatureInUseException extends BaseException {
    public FeatureInUseException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
```

- [ ] **Step 2: Add dynamic subscription property to configurations**

Modify `src/main/resources/application.properties` to add:
```properties
app.subscription.default-free-plan-name=FREE
```

- [ ] **Step 3: Verify build compiles**

Run: `./mvnw clean compile` (or `mvn clean compile` depending on OS)
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit changes**

```bash
git add src/main/java/com/example/demo/common/exceptions/FeatureInUseException.java src/main/resources/application.properties
git commit -m "feat: add FeatureInUseException and default free plan name property"
```

---

### Task 2: Implement Feature Catalog CRUD Foundation

**Files:**
- Create: `src/main/java/com/example/demo/modules/feature/entities/Feature.java`
- Create: `src/main/java/com/example/demo/modules/feature/repositories/FeatureRepository.java`
- Create: `src/main/java/com/example/demo/modules/feature/dto/CreateFeatureRequest.java`
- Create: `src/main/java/com/example/demo/modules/feature/dto/UpdateFeatureRequest.java`
- Create: `src/main/java/com/example/demo/modules/feature/dto/FeatureResponse.java`
- Create: `src/main/java/com/example/demo/modules/feature/mappers/FeatureMapper.java`

- [ ] **Step 1: Write Feature Entity**

Create `src/main/java/com/example/demo/modules/feature/entities/Feature.java`:
```java
package com.example.demo.modules.feature.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "features")
@Data
public class Feature {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "feature_key", unique = true, nullable = false, length = 100)
    private String key;

    @Column(nullable = false)
    private String label;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 2: Write Feature Repository**

Create `src/main/java/com/example/demo/modules/feature/repositories/FeatureRepository.java`:
```java
package com.example.demo.modules.feature.repositories;

import com.example.demo.modules.feature.entities.Feature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeatureRepository extends JpaRepository<Feature, UUID> {
    Optional<Feature> findByKey(String key);
    boolean existsByKey(String key);
}
```

- [ ] **Step 3: Create Feature DTOs**

Create `src/main/java/com/example/demo/modules/feature/dto/CreateFeatureRequest.java`:
```java
package com.example.demo.modules.feature.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateFeatureRequest {
    @NotBlank(message = "Key tính năng không được để trống")
    @Size(max = 100, message = "Key tính năng không được vượt quá 100 ký tự")
    @Pattern(regexp = "^[a-z0-9_]+$", message = "Key tính năng chỉ được chứa chữ thường, số và dấu gạch dưới (snake_case)")
    private String key;

    @NotBlank(message = "Tên hiển thị không được để trống")
    @Size(max = 255, message = "Tên hiển thị không được vượt quá 255 ký tự")
    private String label;

    private String description;

    private Boolean isActive = true;
}
```

Create `src/main/java/com/example/demo/modules/feature/dto/UpdateFeatureRequest.java` (Notice: `key` is excluded to enforce key immutability):
```java
package com.example.demo.modules.feature.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateFeatureRequest {
    @NotBlank(message = "Tên hiển thị không được để trống")
    @Size(max = 255, message = "Tên hiển thị không được vượt quá 255 ký tự")
    private String label;

    private String description;

    private Boolean isActive;
}
```

Create `src/main/java/com/example/demo/modules/feature/dto/FeatureResponse.java`:
```java
package com.example.demo.modules.feature.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class FeatureResponse {
    private UUID id;
    private String key;
    private String label;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 4: Create Feature MapStruct Mapper**

Create `src/main/java/com/example/demo/modules/feature/mappers/FeatureMapper.java`:
```java
package com.example.demo.modules.feature.mappers;

import com.example.demo.common.base.BaseMapper;
import com.example.demo.modules.feature.entities.Feature;
import com.example.demo.modules.feature.dto.CreateFeatureRequest;
import com.example.demo.modules.feature.dto.UpdateFeatureRequest;
import com.example.demo.modules.feature.dto.FeatureResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface FeatureMapper extends BaseMapper<Feature, CreateFeatureRequest, UpdateFeatureRequest, FeatureResponse> {
    @Override
    Feature toEntity(CreateFeatureRequest createRequest);

    @Override
    FeatureResponse toResponse(Feature entity);

    @Override
    void updateEntityFromDto(UpdateFeatureRequest updateRequest, @MappingTarget Feature entity);
}
```

- [ ] **Step 5: Verify build compiles successfully**

Run: `./mvnw clean compile`
Expected: BUILD SUCCESS (MapStruct generated classes created)

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/example/demo/modules/feature/entities/Feature.java src/main/java/com/example/demo/modules/feature/repositories/FeatureRepository.java src/main/java/com/example/demo/modules/feature/dto/ src/main/java/com/example/demo/modules/feature/mappers/FeatureMapper.java
git commit -m "feat: add Feature entity, repository, DTOs, and mapper"
```

---

### Task 3: Implement Feature Service and Web Controller

**Files:**
- Create: `src/main/java/com/example/demo/modules/feature/services/IFeatureService.java`
- Create: `src/main/java/com/example/demo/modules/feature/services/FeatureService.java`
- Create: `src/main/java/com/example/demo/modules/feature/controllers/FeatureController.java`
- Modify: `src/main/java/com/example/demo/modules/subscription/repositories/SubscriptionPlanRepository.java` (to query plans so we can check if a feature key is in use)

- [ ] **Step 1: Define Feature Service Interfaces**

Create `src/main/java/com/example/demo/modules/feature/services/IFeatureService.java`:
```java
package com.example.demo.modules.feature.services;

import com.example.demo.common.base.ICrudService;
import com.example.demo.modules.feature.dto.CreateFeatureRequest;
import com.example.demo.modules.feature.dto.UpdateFeatureRequest;
import com.example.demo.modules.feature.dto.FeatureResponse;
import java.util.UUID;

public interface IFeatureService extends ICrudService<CreateFeatureRequest, UpdateFeatureRequest, FeatureResponse, UUID> {
}
```

- [ ] **Step 2: Add JPA method to SubscriptionPlanRepository**

In `src/main/java/com/example/demo/modules/subscription/repositories/SubscriptionPlanRepository.java`, view it first to verify imports, then modify:
```java
package com.example.demo.modules.subscription.repositories;

import com.example.demo.modules.subscription.entities.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, UUID> {
    Optional<SubscriptionPlan> findByName(String name);
    List<SubscriptionPlan> findByIsActiveTrue();
}
```

- [ ] **Step 3: Implement Feature Service Business Constraints**

Create `src/main/java/com/example/demo/modules/feature/services/FeatureService.java`:
```java
package com.example.demo.modules.feature.services;

import com.example.demo.common.base.BaseService;
import com.example.demo.common.cache.ICacheService;
import com.example.demo.common.exceptions.FeatureInUseException;
import com.example.demo.common.exceptions.ResourceNotFoundException;
import com.example.demo.modules.feature.entities.Feature;
import com.example.demo.modules.feature.repositories.FeatureRepository;
import com.example.demo.modules.feature.dto.CreateFeatureRequest;
import com.example.demo.modules.feature.dto.UpdateFeatureRequest;
import com.example.demo.modules.feature.dto.FeatureResponse;
import com.example.demo.modules.feature.mappers.FeatureMapper;
import com.example.demo.modules.subscription.entities.SubscriptionPlan;
import com.example.demo.modules.subscription.repositories.SubscriptionPlanRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FeatureService 
        extends BaseService<Feature, UUID, CreateFeatureRequest, UpdateFeatureRequest, FeatureResponse>
        implements IFeatureService {

    private final SubscriptionPlanRepository planRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FeatureService(FeatureRepository repository,
                          FeatureMapper mapper,
                          ICacheService cacheService,
                          SubscriptionPlanRepository planRepository) {
        super(repository, mapper, cacheService);
        this.planRepository = planRepository;
    }

    @Override
    protected Class<Feature> getEntityClass() {
        return Feature.class;
    }

    @Override
    @Transactional
    public FeatureResponse create(CreateFeatureRequest requestDto) {
        if (repository.existsById(UUID.nameUUIDFromBytes(requestDto.getKey().getBytes())) 
            || ((FeatureRepository) repository).existsByKey(requestDto.getKey())) {
            throw new IllegalArgumentException("Feature với key '" + requestDto.getKey() + "' đã tồn tại.");
        }
        return super.create(requestDto);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Feature feature = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tính năng để xóa. ID: " + id));

        String key = feature.getKey();
        List<SubscriptionPlan> activePlans = planRepository.findAll();

        for (SubscriptionPlan plan : activePlans) {
            if (plan.getFeatures() != null && !plan.getFeatures().trim().isEmpty()) {
                try {
                    Map<String, Object> featureMap = objectMapper.readValue(plan.getFeatures(), new TypeReference<Map<String, Object>>() {});
                    if (featureMap.containsKey(key) && Boolean.TRUE.equals(featureMap.get(key))) {
                        throw new FeatureInUseException("Không thể xóa tính năng này vì nó đang hoạt động trong gói cước: " + plan.getName());
                    }
                } catch (Exception e) {
                    // Log parse error but proceed or treat as constraint failure
                }
            }
        }

        super.delete(id);
    }
}
```

- [ ] **Step 4: Create Admin REST Controller for Feature**

Create `src/main/java/com/example/demo/modules/feature/controllers/FeatureController.java`:
```java
package com.example.demo.modules.feature.controllers;

import com.example.demo.common.base.BaseController;
import com.example.demo.common.security.annotations.IsAdmin;
import com.example.demo.common.security.annotations.IsAuthenticated;
import com.example.demo.modules.feature.dto.CreateFeatureRequest;
import com.example.demo.modules.feature.dto.UpdateFeatureRequest;
import com.example.demo.modules.feature.dto.FeatureResponse;
import com.example.demo.modules.feature.services.IFeatureService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequestMapping("/api/features")
@IsAdmin
public class FeatureController extends BaseController<CreateFeatureRequest, UpdateFeatureRequest, FeatureResponse, UUID> {

    public FeatureController(IFeatureService service) {
        super(service);
    }

    @Override
    @IsAuthenticated
    public ResponseEntity<Page<FeatureResponse>> getAll(Pageable pageable) {
        return super.getAll(pageable);
    }

    @Override
    @IsAuthenticated
    public ResponseEntity<FeatureResponse> getById(@PathVariable UUID id) {
        return super.getById(id);
    }
}
```

- [ ] **Step 5: Verify build compiles cleanly**

Run: `./mvnw clean compile`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/example/demo/modules/feature/services/ src/main/java/com/example/demo/modules/feature/controllers/ src/main/java/com/example/demo/modules/subscription/repositories/SubscriptionPlanRepository.java
git commit -m "feat: implement FeatureService and FeatureController with delete restrictions"
```

---

### Task 4: Write Unit Tests for Feature Catalog

**Files:**
- Create: `src/test/java/com/example/demo/modules/feature/services/FeatureServiceTest.java`

- [ ] **Step 1: Write JUnit tests for Feature Catalog business logic**

Create `src/test/java/com/example/demo/modules/feature/services/FeatureServiceTest.java`:
```java
package com.example.demo.modules.feature.services;

import com.example.demo.common.cache.ICacheService;
import com.example.demo.common.exceptions.FeatureInUseException;
import com.example.demo.common.exceptions.ResourceNotFoundException;
import com.example.demo.modules.feature.entities.Feature;
import com.example.demo.modules.feature.repositories.FeatureRepository;
import com.example.demo.modules.feature.dto.CreateFeatureRequest;
import com.example.demo.modules.feature.dto.FeatureResponse;
import com.example.demo.modules.feature.mappers.FeatureMapper;
import com.example.demo.modules.subscription.entities.SubscriptionPlan;
import com.example.demo.modules.subscription.repositories.SubscriptionPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FeatureServiceTest {

    @Mock
    private FeatureRepository featureRepository;

    @Mock
    private FeatureMapper featureMapper;

    @Mock
    private ICacheService cacheService;

    @Mock
    private SubscriptionPlanRepository planRepository;

    private FeatureService featureService;

    @BeforeEach
    public void setUp() {
        featureService = new FeatureService(featureRepository, featureMapper, cacheService, planRepository);
    }

    @Test
    public void testCreateFeature_KeyAlreadyExists_ThrowsException() {
        CreateFeatureRequest request = new CreateFeatureRequest();
        request.setKey("test_feature");

        when(featureRepository.existsByKey("test_feature")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> {
            featureService.create(request);
        });

        verify(featureRepository, never()).save(any());
    }

    @Test
    public void testDeleteFeature_FeatureInUse_ThrowsFeatureInUseException() {
        UUID featureId = UUID.randomUUID();
        Feature feature = new Feature();
        feature.setId(featureId);
        feature.setKey("slack_notifications");

        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setName("PRO");
        plan.setFeatures("{\"slack_notifications\": true}");

        when(featureRepository.findById(featureId)).thenReturn(Optional.of(feature));
        when(planRepository.findAll()).thenReturn(Collections.singletonList(plan));

        assertThrows(FeatureInUseException.class, () -> {
            featureService.delete(featureId);
        });

        verify(featureRepository, never()).deleteById(any());
    }

    @Test
    public void testDeleteFeature_FeatureNotUsed_Success() {
        UUID featureId = UUID.randomUUID();
        Feature feature = new Feature();
        feature.setId(featureId);
        feature.setKey("slack_notifications");

        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setName("FREE");
        plan.setFeatures("{\"slack_notifications\": false}");

        when(featureRepository.findById(featureId)).thenReturn(Optional.of(feature));
        when(planRepository.findAll()).thenReturn(Collections.singletonList(plan));
        when(featureRepository.existsById(featureId)).thenReturn(true);

        featureService.delete(featureId);

        verify(featureRepository, times(1)).deleteById(featureId);
    }
}
```

- [ ] **Step 2: Run Unit Tests and Verify Pass**

Run: `./mvnw test -Dtest=FeatureServiceTest`
Expected: Tests pass successfully (100% PASS)

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/example/demo/modules/feature/services/FeatureServiceTest.java
git commit -m "test: add unit tests for FeatureService"
```

---

### Task 5: Refactor SubscriptionService Default Plan

**Files:**
- Modify: `src/main/java/com/example/demo/modules/subscription/services/SubscriptionService.java`

- [ ] **Step 1: Refactor SubscriptionService to dynamically load Default Free Plan Name**

Open `src/main/java/com/example/demo/modules/subscription/services/SubscriptionService.java` and modify to inject and use the configurable free plan name:
```java
        private final SubscriptionRepository subscriptionRepository;
        private final SubscriptionPlanRepository planRepository;
        private final UserRepository userRepository;
        private final DashboardCacheService dashboardCacheService;
        private final ICacheService cacheService;
        private final PaymentLogsRepository paymentLogsRepository;

        @org.springframework.beans.factory.annotation.Value("${app.subscription.default-free-plan-name:FREE}")
        private String defaultFreePlanName;
```
Modify `subscribeFreePlan` method:
```java
        @Transactional
        @Override
        public void subscribeFreePlan(User user) {
                SubscriptionPlan freePlan = planRepository.findByName(defaultFreePlanName)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                 "Lỗi hệ thống: Không tìm thấy cấu hình gói default: " + defaultFreePlanName));

                // 1. Cập nhật thông tin gói trên User entity
                user.setSubscriptionPlan(freePlan);
                user.setPlanType(freePlan.getName());
                userRepository.save(user);

                // 2. Tìm subscription hiện tại đang ACTIVE hoặc tạo mới
                Subscription subscription = subscriptionRepository
                                .findByUserIdAndStatus(user.getId(), SubscriptionStatus.ACTIVE)
                                .orElse(new Subscription());

                subscription.setUser(user);
                subscription.setPlan(freePlan);
                subscription.setPlanName(freePlan.getName());
                subscription.setPlanPrice(BigDecimal.ZERO);
                subscription.setCurrency(freePlan.getCurrency() != null ? freePlan.getCurrency() : "VND");
                subscription.setMaxMonitors(freePlan.getMaxMonitors());
                subscription.setMinInterval(freePlan.getMinInterval());
                subscription.setStartDate(LocalDateTime.now());
                subscription.setCurrentPeriodEnd(LocalDateTime.now().plusYears(10)); // Gói FREE hiệu lực lâu dài
                subscription.setBillingCycle(freePlan.getBillingCycle());
                subscription.setStatus(SubscriptionStatus.ACTIVE);
                subscription.setPaymentStatus(PaymentStatus.FREE);

                subscriptionRepository.save(subscription);
                dashboardCacheService.clearUserDashboardCache(user.getId());
        }
```

- [ ] **Step 2: Run all tests to make sure there are no breakages**

Run: `./mvnw test`
Expected: All tests pass successfully (including `SubscriptionExpirySchedulerTest`)

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/example/demo/modules/subscription/services/SubscriptionService.java
git commit -m "refactor: configure default free plan name dynamically"
```

---

### Task 6: Refactor Data Seeder to Dynamic Seeding

**Files:**
- Modify: `src/main/java/com/example/demo/common/config/DataInitializer.java`

- [ ] **Step 1: Update DataInitializer to seed Features catalog first**

Modify `src/main/java/com/example/demo/common/config/DataInitializer.java` to inject `FeatureRepository` and seed default features:
```java
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
            // 1. Seed dynamic features catalog
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
```

- [ ] **Step 2: Clean and build the database locally**

Run: `./mvnw clean test`
Expected: BUILD SUCCESS (All tests pass and schema initialization functions successfully)

- [ ] **Step 3: Commit changes**

```bash
git add src/main/java/com/example/demo/common/config/DataInitializer.java
git commit -m "refactor: seed dynamic feature catalog and associate them with default plans"
```
