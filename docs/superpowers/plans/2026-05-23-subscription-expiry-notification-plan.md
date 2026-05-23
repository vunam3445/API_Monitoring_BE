# Subscription Expiry Notification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tự động quét hàng ngày vào lúc 07:00 AM để tìm các gói đăng ký trả phí sắp hết hạn trong 3 ngày và gửi email thông báo nhắc gia hạn thông qua hàng đợi RabbitMQ bất đồng bộ.

**Architecture:** Sử dụng Spring Scheduler để kích hoạt cron job định kỳ quét Database PostgreSQL. Các bản ghi tìm thấy sẽ được chuyển hóa thành JSON Event và đẩy vào RabbitMQ Queue (`subscription.expiry.queue`). Consumer sẽ lắng nghe từ queue này độc lập để gửi HTML Email, giúp hệ thống hoạt động ổn định và tin cậy cao.

**Tech Stack:** Java 17, Spring Boot, Spring AMQP (RabbitMQ), Spring Data JPA (PostgreSQL), Jakarta Mail (SMTP).

---

### Task 1: Cấu hình RabbitMQ Queue
**Files:**
- Create: `src/main/java/com/example/demo/modules/subscription/config/SubscriptionExpiryMQConfig.java`

- [ ] **Step 1: Tạo tệp cấu hình RabbitMQ cho thông báo hết hạn gói**
  Tạo tệp `SubscriptionExpiryMQConfig.java` để khai báo hàng đợi `subscription.expiry.queue`.
  ```java
  package com.example.demo.modules.subscription.config;

  import org.springframework.amqp.core.Queue;
  import org.springframework.context.annotation.Bean;
  import org.springframework.context.annotation.Configuration;

  @Configuration
  public class SubscriptionExpiryMQConfig {

      public static final String EXPIRY_QUEUE = "subscription.expiry.queue";

      @Bean
      public Queue subscriptionExpiryQueue() {
          return new Queue(EXPIRY_QUEUE, true); // Durable = true để giữ tin nhắn khi restart RabbitMQ
      }
  }
  ```

- [ ] **Step 2: Biên dịch và kiểm thử cú pháp**
  Chạy lệnh: `.\mvnw.cmd compile`
  Yêu cầu: BUILD SUCCESS

---

### Task 2: Tạo DTO Record sự kiện hết hạn
**Files:**
- Create: `src/main/java/com/example/demo/modules/subscription/dto/SubscriptionExpiryEvent.java`

- [ ] **Step 1: Định nghĩa sự kiện SubscriptionExpiryEvent bằng Java Record**
  Tạo tệp `SubscriptionExpiryEvent.java` chứa các trường dữ liệu cần thiết phục vụ việc dựng mẫu HTML Email ở phía consumer.
  ```java
  package com.example.demo.modules.subscription.dto;

  import java.io.Serializable;

  public record SubscriptionExpiryEvent(
      String subscriptionId,
      String userEmail,
      String userName,
      String planName,
      String expiryDate
  ) implements Serializable {}
  ```

- [ ] **Step 2: Biên dịch dự án**
  Chạy lệnh: `.\mvnw.cmd compile`
  Yêu cầu: BUILD SUCCESS

---

### Task 3: Cập nhật SubscriptionRepository với Jpa Custom Query
**Files:**
- Modify: `src/main/java/com/example/demo/modules/subscription/repositories/SubscriptionRepository.java`

- [ ] **Step 1: Thêm phương thức truy vấn tối ưu**
  Sửa tệp `SubscriptionRepository.java` bằng cách bổ sung phương thức `findActivePaidSubscriptionsExpiringBetween` để lấy danh sách gói trả phí sắp hết hạn với cơ chế `JOIN FETCH` nhằm chống lỗi N+1 Query.
  ```java
      @Query("SELECT s FROM Subscription s " +
             "JOIN FETCH s.user " +
             "JOIN FETCH s.plan " +
             "WHERE s.status = :status " +
             "AND s.currentPeriodEnd >= :startDate " +
             "AND s.currentPeriodEnd <= :endDate " +
             "AND s.plan.price > 0")
      List<Subscription> findActivePaidSubscriptionsExpiringBetween(
              @Param("status") SubscriptionStatus status,
              @Param("startDate") LocalDateTime startDate,
              @Param("endDate") LocalDateTime endDate);
  ```

- [ ] **Step 2: Biên dịch kiểm chứng**
  Chạy lệnh: `.\mvnw.cmd compile`
  Yêu cầu: BUILD SUCCESS

---

### Task 4: Nâng cấp EmailSenderService để hỗ trợ gửi Email nhắc gia hạn
**Files:**
- Modify: `src/main/java/com/example/demo/modules/alert/services/EmailSenderService.java`
- Modify: `src/main/java/com/example/demo/modules/alert/services/EmailSenderServiceImpl.java`

- [ ] **Step 1: Thêm khai báo phương thức gửi email hết hạn vào Interface**
  Sửa tệp `EmailSenderService.java` để bổ sung định nghĩa:
  ```java
  void sendSubscriptionExpiryEmail(String recipient, String userName, String planName, String expiryDate);
  ```

- [ ] **Step 2: Triển khai phương thức gửi email HTML nhắc gia hạn**
  Sửa tệp `EmailSenderServiceImpl.java` để triển khai phương thức và xây dựng mẫu giao diện email HTML.
  ```java
      @Override
      public void sendSubscriptionExpiryEmail(String recipient, String userName, String planName, String expiryDate) {
          String subject = String.format("[API Monitoring] Gia hạn gói dịch vụ %s sắp hết hạn", planName);
          String content = buildSubscriptionExpiryHtmlContent(userName, planName, expiryDate);
          sendHtmlEmail(recipient, subject, content);
      }

      private String buildSubscriptionExpiryHtmlContent(String userName, String planName, String expiryDate) {
          return String.format(
              "<div style=\"font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f8fafc; padding: 40px 10px; color: #1e293b;\">" +
              "    <div style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1), 0 2px 4px -2px rgba(0,0,0,0.1); border: 1px solid #e2e8f0;\">" +
              "        <div style=\"background: linear-gradient(135deg, #4f46e5 0%, #3b82f6 100%); padding: 35px 20px; text-align: center;\">" +
              "            <h1 style=\"color: #ffffff; margin: 0; font-size: 24px; font-weight: 800; letter-spacing: 0.5px;\">API Monitoring</h1>" +
              "            <p style=\"color: #e0e7ff; margin: 5px 0 0 0; font-size: 14px;\">Hệ thống giám sát hiệu năng API thông minh</p>" +
              "        </div>" +
              "        <div style=\"padding: 40px 30px;\">" +
              "            <h2 style=\"color: #0f172a; margin-top: 0; font-size: 20px; font-weight: 700;\">Xin chào, %s!</h2>" +
              "            <p style=\"font-size: 15px; line-height: 1.6; color: #475569;\">" +
              "                Chúng tôi xin thông báo gói dịch vụ trả phí <strong>%s</strong> của bạn trên hệ thống <strong>API Monitoring</strong> sẽ hết hạn vào ngày <span style=\"color: #ef4444; font-weight: 700;\">%s</span> (3 ngày nữa)." +
              "            </p>" +
              "            <div style=\"background-color: #fffbeb; border-left: 4px solid #f59e0b; padding: 20px; border-radius: 8px; margin: 25px 0;\">" +
              "                <h4 style=\"color: #b45309; margin: 0 0 8px 0; font-size: 15px; font-weight: 700;\">⚠️ Lưu ý quan trọng</h4>" +
              "                <p style=\"margin: 0; font-size: 14px; line-height: 1.5; color: #78350f;\">" +
              "                    Sau ngày hết hạn, tài khoản của bạn sẽ tự động chuyển về gói <strong>FREE</strong>. Các giới hạn như số lượng API giám sát, thời gian chu kỳ quét sẽ bị thu hẹp đáng kể, gây gián đoạn việc giám sát hệ thống của bạn." +
              "                </p>" +
              "            </div>" +
              "            <p style=\"font-size: 15px; line-height: 1.6; color: #475569; text-align: center; margin-top: 30px;\">" +
              "                Hãy gia hạn hoặc nâng cấp ngay hôm nay để duy trì kết nối giám sát 24/7 không bị ngắt quãng!" +
              "            </p>" +
              "            <div style=\"text-align: center; margin: 35px 0;\">" +
              "                <a href=\"http://localhost:3000/admin/billing\" style=\"background-color: #4f46e5; color: #ffffff; text-decoration: none; padding: 14px 35px; border-radius: 8px; font-weight: bold; font-size: 15px; display: inline-block; box-shadow: 0 4px 6px -1px rgba(79, 70, 229, 0.2);\">" +
              "                    Gia Hạn Gói Ngay" +
              "                </a>" +
              "            </div>" +
              "        </div>" +
              "        <div style=\"background-color: #f1f5f9; padding: 25px; text-align: center; border-top: 1px solid #e2e8f0; font-size: 12px; color: #94a3b8;\">" +
              "            <p style=\"margin: 0 0 5px 0;\">Đây là email tự động từ hệ thống API Monitoring, vui lòng không trả lời trực tiếp email này.</p>" +
              "            <p style=\"margin: 0;\">&copy; 2026 API Monitoring Team. All rights reserved.</p>" +
              "        </div>" +
              "    </div>" +
              "</div>",
              userName, planName, expiryDate
          );
      }
  ```

- [ ] **Step 3: Biên dịch dự án**
  Chạy lệnh: `.\mvnw.cmd compile`
  Yêu cầu: BUILD SUCCESS

---

### Task 5: Triển khai SubscriptionExpiryScheduler
**Files:**
- Create: `src/main/java/com/example/demo/modules/subscription/services/SubscriptionExpiryScheduler.java`

- [ ] **Step 1: Tạo Class SubscriptionExpiryScheduler**
  Tạo tệp `SubscriptionExpiryScheduler.java` để thực thi Cron quét DB và đẩy các message sự kiện vào queue.
  ```java
  package com.example.demo.modules.subscription.services;

  import com.example.demo.modules.subscription.config.SubscriptionExpiryMQConfig;
  import com.example.demo.modules.subscription.dto.SubscriptionExpiryEvent;
  import com.example.demo.modules.subscription.entities.Subscription;
  import com.example.demo.modules.subscription.enums.SubscriptionStatus;
  import com.example.demo.modules.subscription.repositories.SubscriptionRepository;
  import lombok.RequiredArgsConstructor;
  import lombok.extern.slf4j.Slf4j;
  import org.springframework.amqp.rabbit.core.RabbitTemplate;
  import org.springframework.scheduling.annotation.Scheduled;
  import org.springframework.stereotype.Service;

  import java.time.LocalDateTime;
  import java.time.LocalTime;
  import java.time.format.DateTimeFormatter;
  import java.util.List;

  @Service
  @RequiredArgsConstructor
  @Slf4j
  public class SubscriptionExpiryScheduler {

      private final SubscriptionRepository subscriptionRepository;
      private final RabbitTemplate rabbitTemplate;
      private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

      @Scheduled(cron = "0 0 7 * * ?", zone = "Asia/Ho_Chi_Minh") // 7h sáng hàng ngày (Giờ Hà Nội)
      public void scanAndNotifyExpiringSubscriptions() {
          log.info("[SubscriptionExpiryScheduler] Khởi chạy quét gói dịch vụ sắp hết hạn sau 3 ngày...");

          LocalDateTime startDate = LocalDateTime.now().plusDays(3).with(LocalTime.MIN);
          LocalDateTime endDate = LocalDateTime.now().plusDays(3).with(LocalTime.MAX);

          List<Subscription> expiringSubs = subscriptionRepository
                  .findActivePaidSubscriptionsExpiringBetween(SubscriptionStatus.ACTIVE, startDate, endDate);

          log.info("[SubscriptionExpiryScheduler] Tìm thấy {} subscriptions trả phí sắp hết hạn vào ngày {}", 
                  expiringSubs.size(), startDate.format(DATE_FORMATTER));

          for (Subscription sub : expiringSubs) {
              if (sub.getUser() == null || sub.getUser().getEmail() == null) continue;

              SubscriptionExpiryEvent event = new SubscriptionExpiryEvent(
                      sub.getId().toString(),
                      sub.getUser().getEmail(),
                      sub.getUser().getFullName() != null ? sub.getUser().getFullName() : "Khách hàng",
                      sub.getPlanName() != null ? sub.getPlanName() : sub.getPlan().getName(),
                      sub.getCurrentPeriodEnd().format(DATE_FORMATTER)
              );

              rabbitTemplate.convertAndSend(SubscriptionExpiryMQConfig.EXPIRY_QUEUE, event);
              log.info("[SubscriptionExpiryScheduler] Đã đẩy sự kiện hết hạn vào RabbitMQ cho user: {}", sub.getUser().getEmail());
          }
      }
  }
  ```

- [ ] **Step 2: Biên dịch dự án**
  Chạy lệnh: `.\mvnw.cmd compile`
  Yêu cầu: BUILD SUCCESS

---

### Task 6: Triển khai SubscriptionExpiryConsumer
**Files:**
- Create: `src/main/java/com/example/demo/modules/subscription/services/SubscriptionExpiryConsumer.java`

- [ ] **Step 1: Tạo Class SubscriptionExpiryConsumer**
  Tạo tệp `SubscriptionExpiryConsumer.java` lắng nghe queue `subscription.expiry.queue` để tiêu thụ message và gọi EmailSenderService.
  ```java
  package com.example.demo.modules.subscription.services;

  import com.example.demo.modules.alert.services.EmailSenderService;
  import com.example.demo.modules.subscription.config.SubscriptionExpiryMQConfig;
  import com.example.demo.modules.subscription.dto.SubscriptionExpiryEvent;
  import lombok.RequiredArgsConstructor;
  import lombok.extern.slf4j.Slf4j;
  import org.springframework.amqp.rabbit.annotation.RabbitListener;
  import org.springframework.stereotype.Service;

  @Service
  @RequiredArgsConstructor
  @Slf4j
  public class SubscriptionExpiryConsumer {

      private final EmailSenderService emailSenderService;

      @RabbitListener(queues = SubscriptionExpiryMQConfig.EXPIRY_QUEUE)
      public void consumeExpiryEvent(SubscriptionExpiryEvent event) {
          log.info("[SubscriptionExpiryConsumer] Nhận sự kiện nhắc gia hạn gửi cho email: {}", event.userEmail());
          try {
              emailSenderService.sendSubscriptionExpiryEmail(
                      event.userEmail(),
                      event.userName(),
                      event.planName(),
                      event.expiryDate()
              );
              log.info("[SubscriptionExpiryConsumer] Đã gửi mail nhắc gia hạn thành công cho email: {}", event.userEmail());
          } catch (Exception e) {
              log.error("[SubscriptionExpiryConsumer] Lỗi khi xử lý gửi email cho {}: {}", event.userEmail(), e.getMessage());
              throw e; // Throw lỗi để kích hoạt cơ chế retry của RabbitMQ
          }
      }
  }
  ```

- [ ] **Step 2: Biên dịch dự án**
  Chạy lệnh: `.\mvnw.cmd compile`
  Yêu cầu: BUILD SUCCESS

---

### Task 7: Viết JUnit Integration Test để tự động xác minh tính đúng đắn
**Files:**
- Create: `src/test/java/com/example/demo/modules/subscription/services/SubscriptionExpirySchedulerTest.java`

- [ ] **Step 1: Tạo Unit Test kiểm thử logic**
  Tạo tệp kiểm thử `SubscriptionExpirySchedulerTest.java` giả lập cơ sở dữ liệu và xác nhận Scheduler truy vấn và đẩy tin nhắn vào RabbitTemplate chính xác.
  ```java
  package com.example.demo.modules.subscription.services;

  import com.example.demo.modules.subscription.config.SubscriptionExpiryMQConfig;
  import com.example.demo.modules.subscription.dto.SubscriptionExpiryEvent;
  import com.example.demo.modules.subscription.entities.Subscription;
  import com.example.demo.modules.subscription.entities.SubscriptionPlan;
  import com.example.demo.modules.subscription.enums.SubscriptionStatus;
  import com.example.demo.modules.subscription.repositories.SubscriptionRepository;
  import com.example.demo.modules.user.entities.User;
  import org.junit.jupiter.api.Test;
  import org.junit.jupiter.api.extension.ExtendWith;
  import org.mockito.ArgumentCaptor;
  import org.mockito.InjectMocks;
  import org.mockito.Mock;
  import org.mockito.junit.jupiter.MockitoExtension;
  import org.springframework.amqp.rabbit.core.RabbitTemplate;

  import java.math.BigDecimal;
  import java.time.LocalDateTime;
  import java.util.Collections;
  import java.util.UUID;

  import static org.junit.jupiter.api.Assertions.assertEquals;
  import static org.mockito.ArgumentMatchers.any;
  import static org.mockito.ArgumentMatchers.eq;
  import static org.mockito.Mockito.*;

  @ExtendWith(MockitoExtension.class)
  public class SubscriptionExpirySchedulerTest {

      @Mock
      private SubscriptionRepository subscriptionRepository;

      @Mock
      private RabbitTemplate rabbitTemplate;

      @InjectMocks
      private SubscriptionExpiryScheduler scheduler;

      @Test
      public void testScanAndNotifyExpiringSubscriptions_Success() {
          // Arrange
          User user = new User();
          user.setEmail("test@example.com");
          user.setFullName("Nguyen Van A");

          SubscriptionPlan plan = new SubscriptionPlan();
          plan.setName("PRO PLAN");
          plan.setPrice(BigDecimal.valueOf(199000));

          Subscription subscription = new Subscription();
          subscription.setId(UUID.randomUUID());
          subscription.setUser(user);
          subscription.setPlan(plan);
          subscription.setCurrentPeriodEnd(LocalDateTime.now().plusDays(3));

          when(subscriptionRepository.findActivePaidSubscriptionsExpiringBetween(
                  eq(SubscriptionStatus.ACTIVE), any(), any()))
                  .thenReturn(Collections.singletonList(subscription));

          // Act
          scheduler.scanAndNotifyExpiringSubscriptions();

          // Assert
          ArgumentCaptor<SubscriptionExpiryEvent> eventCaptor = ArgumentCaptor.forClass(SubscriptionExpiryEvent.class);
          verify(rabbitTemplate, times(1)).convertAndSend(
                  eq(SubscriptionExpiryMQConfig.EXPIRY_QUEUE), eventCaptor.capture());

          SubscriptionExpiryEvent capturedEvent = eventCaptor.getValue();
          assertEquals("test@example.com", capturedEvent.userEmail());
          assertEquals("Nguyen Van A", capturedEvent.userName());
          assertEquals("PRO PLAN", capturedEvent.planName());
      }
  }
  ```

- [ ] **Step 2: Chạy toàn bộ Unit Tests**
  Chạy lệnh: `.\mvnw.cmd test -Dtest=SubscriptionExpirySchedulerTest`
  Yêu cầu: Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 (BUILD SUCCESS)
