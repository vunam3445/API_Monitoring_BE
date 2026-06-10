# SSE Multi-Instance Redis Pub/Sub Notification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Triển khai gửi thông báo sự cố API (Down/Warning/Recovery) qua cả DB và kênh SSE thời gian thực, hoạt động ổn định trong môi trường phân tán đa instance nhờ đồng bộ qua Redis Pub/Sub.

**Architecture:** 
1. `IncidentService` tạo yêu cầu thông báo bất đồng bộ qua `NotificationService.sendNotification`.
2. `NotificationService` lưu DB và gửi qua RabbitMQ tới `NotificationBroadcastConsumer`.
3. Consumer gọi `NotificationSseService.sendNotification(userId, response)`.
4. `NotificationSseServiceImpl` phát tán sự kiện lên Redis Pub/Sub channel `sse:notifications`.
5. `RedisNotificationSubscriber` trên toàn bộ các instance lắng nghe Redis channel và thực hiện đẩy dữ liệu xuống socket của client online cục bộ qua `sendNotificationLocal`.

**Tech Stack:** Spring Boot 3.x, Spring Data Redis, RabbitMQ, Lombok, JUnit 5, Mockito.

---

## 📂 File Structure

### Created Files:
* `src/main/java/com/example/demo/modules/notification/dto/SseNotificationPayload.java` (DTO trung gian chuyển tải dữ liệu qua Redis)
* `src/main/java/com/example/demo/common/config/RedisPubSubConfig.java` (Cấu hình Redis Message Container & Topic)
* `src/main/java/com/example/demo/modules/notification/services/RedisNotificationSubscriber.java` (Đăng ký lắng nghe kênh Redis Pub/Sub)
* `src/test/java/com/example/demo/modules/notification/services/NotificationSseServiceTest.java` (Unit test cho SSE & Pub/Sub)

### Modified Files:
* `src/main/java/com/example/demo/modules/notification/services/NotificationSseService.java` (Bổ sung method `sendNotificationLocal`)
* `src/main/java/com/example/demo/modules/notification/services/NotificationSseServiceImpl.java` (Cập nhật logic publish Redis và đẩy local)
* `src/main/java/com/example/demo/modules/alert/services/IncidentService.java` (Tích hợp tạo thông báo khi xảy ra sự cố API)

---

## 🛠️ Tasks

### Task 1: Tạo DTO payload Redis Pub/Sub

**Files:**
* Create: `src/main/java/com/example/demo/modules/notification/dto/SseNotificationPayload.java`

- [ ] **Step 1: Tạo lớp DTO chứa thông tin payload**
  
  Tạo file `src/main/java/com/example/demo/modules/notification/dto/SseNotificationPayload.java` với nội dung sau:
  ```java
  package com.example.demo.modules.notification.dto;

  import lombok.AllArgsConstructor;
  import lombok.Data;
  import lombok.NoArgsConstructor;
  import java.io.Serializable;
  import java.util.UUID;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public class SseNotificationPayload implements Serializable {
      private static final long serialVersionUID = 1L;
      
      private UUID userId;
      private UserNotificationResponse notification;
  }
  ```

- [ ] **Step 2: Commit thay đổi**
  ```bash
  git add src/main/java/com/example/demo/modules/notification/dto/SseNotificationPayload.java
  git commit -m "feat: add SseNotificationPayload DTO for Redis Pub/Sub"
  ```

---

### Task 2: Cấu hình Redis Pub/Sub Container & Subscriber

**Files:**
* Create: `src/main/java/com/example/demo/common/config/RedisPubSubConfig.java`
* Create: `src/main/java/com/example/demo/modules/notification/services/RedisNotificationSubscriber.java`

- [ ] **Step 1: Tạo class subscriber nhận tin nhắn từ Redis**
  
  Tạo file `src/main/java/com/example/demo/modules/notification/services/RedisNotificationSubscriber.java` như sau:
  ```java
  package com.example.demo.modules.notification.services;

  import com.example.demo.modules.notification.dto.SseNotificationPayload;
  import com.fasterxml.jackson.databind.ObjectMapper;
  import lombok.RequiredArgsConstructor;
  import lombok.extern.slf4j.Slf4j;
  import org.springframework.data.redis.connection.Message;
  import org.springframework.data.redis.connection.MessageListener;
  import org.springframework.stereotype.Component;

  import java.io.IOException;

  @Component
  @RequiredArgsConstructor
  @Slf4j
  public class RedisNotificationSubscriber implements MessageListener {

      private final ObjectMapper objectMapper;
      private final NotificationSseService notificationSseService;

      @Override
      public void onMessage(Message message, byte[] pattern) {
          try {
              SseNotificationPayload payload = objectMapper.readValue(message.getBody(), SseNotificationPayload.class);
              log.info("[RedisPubSub] Received SSE notification event for userId={}", payload.getUserId());
              notificationSseService.sendNotificationLocal(payload.getUserId(), payload.getNotification());
          } catch (IOException e) {
              log.error("[RedisPubSub] Failed to deserialize message body: {}", e.getMessage());
          } catch (Exception e) {
              log.error("[RedisPubSub] Error processing Redis message: {}", e.getMessage());
          }
      }
  }
  ```

- [ ] **Step 2: Tạo lớp cấu hình đăng ký Redis Listener Container**
  
  Tạo file `src/main/java/com/example/demo/common/config/RedisPubSubConfig.java` như sau:
  ```java
  package com.example.demo.common.config;

  import com.example.demo.modules.notification.services.RedisNotificationSubscriber;
  import org.springframework.context.annotation.Bean;
  import org.springframework.context.annotation.Configuration;
  import org.springframework.data.redis.connection.RedisConnectionFactory;
  import org.springframework.data.redis.listener.ChannelTopic;
  import org.springframework.data.redis.listener.RedisMessageListenerContainer;
  import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

  @Configuration
  public class RedisPubSubConfig {

      public static final String SSE_CHANNEL = "sse:notifications";

      @Bean
      public RedisMessageListenerContainer redisMessageListenerContainer(
              RedisConnectionFactory connectionFactory,
              MessageListenerAdapter listenerAdapter) {
          RedisMessageListenerContainer container = new RedisMessageListenerContainer();
          container.setConnectionFactory(connectionFactory);
          container.addMessageListener(listenerAdapter, new ChannelTopic(SSE_CHANNEL));
          return container;
      }

      @Bean
      public MessageListenerAdapter listenerAdapter(RedisNotificationSubscriber subscriber) {
          return new MessageListenerAdapter(subscriber, "onMessage");
      }
  }
  ```

- [ ] **Step 3: Commit thay đổi**
  ```bash
  git add src/main/java/com/example/demo/common/config/RedisPubSubConfig.java src/main/java/com/example/demo/modules/notification/services/RedisNotificationSubscriber.java
  git commit -m "feat: configure Redis PubSub container and subscriber"
  ```

---

### Task 3: Cập nhật NotificationSseService & NotificationSseServiceImpl

**Files:**
* Modify: `src/main/java/com/example/demo/modules/notification/services/NotificationSseService.java`
* Modify: `src/main/java/com/example/demo/modules/notification/services/NotificationSseServiceImpl.java`

- [ ] **Step 1: Khai báo phương thức `sendNotificationLocal` trong interface**
  
  Mở `src/main/java/com/example/demo/modules/notification/services/NotificationSseService.java` và thêm khai báo:
  ```java
      /**
       * Đẩy thông báo cục bộ qua SseEmitter đang kết nối trực tiếp với instance này.
       */
      void sendNotificationLocal(UUID userId, UserNotificationResponse notification);
  ```

- [ ] **Step 2: Cập nhật triển khai logic tại `NotificationSseServiceImpl.java`**
  
  Chỉnh sửa `src/main/java/com/example/demo/modules/notification/services/NotificationSseServiceImpl.java` để inject `RedisTemplate` và phân tách hành vi gửi tin:
  
  ```java
      // Inject thêm RedisTemplate
      private final org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

      // Constructor của Lombok sẽ tự sinh nếu sử dụng các annotation như @RequiredArgsConstructor hoặc cần khai báo thủ công tùy thuộc phong cách code
  ```
  *(Lưu ý: class hiện tại chưa có Constructor Lombok. Ta sẽ sửa class thủ công để thêm trường `redisTemplate` và cập nhật constructor/dependency injection).*
  
  Chuyển đổi logic hàm gửi tin:
  * Hàm `sendNotification` thay vì ghi thẳng qua emitter cục bộ, sẽ bắn qua `redisTemplate.convertAndSend(RedisPubSubConfig.SSE_CHANNEL, payload)`.
  * Hàm `sendNotificationLocal` thực hiện đẩy socket cục bộ.

- [ ] **Step 3: Commit thay đổi**
  ```bash
  git add src/main/java/com/example/demo/modules/notification/services/NotificationSseService.java src/main/java/com/example/demo/modules/notification/services/NotificationSseServiceImpl.java
  git commit -m "feat: integrate Redis Pub/Sub into NotificationSseServiceImpl"
  ```

---

### Task 4: Tích hợp đẩy thông báo sự cố trong IncidentService

**Files:**
* Modify: `src/main/java/com/example/demo/modules/alert/services/IncidentService.java`

- [ ] **Step 1: Bổ sung dependencies vào `IncidentService.java`**
  
  Thêm khai báo dependencies:
  ```java
      private final com.example.demo.modules.user.repositories.UserRepository userRepository;
      private final com.example.demo.modules.notification.services.NotificationService notificationService;
  ```

- [ ] **Step 2: Viết hàm tạo yêu cầu thông báo `triggerWebNotification`**
  
  Thêm hàm sau vào `IncidentService.java`:
  ```java
      private void triggerWebNotification(Incident incident) {
          try {
              UUID userId = incident.getMonitor().getUserId();
              String userEmail = incident.getMonitor().getUser() != null 
                      ? incident.getMonitor().getUser().getEmail() 
                      : userRepository.findById(userId).map(com.example.demo.modules.user.entities.User::getEmail).orElse(null);

              if (userEmail == null) {
                  log.warn("[IncidentService] Cannot find email for userId={} to send web notification", userId);
                  return;
              }

              com.example.demo.modules.notification.dto.SendNotificationRequest request = 
                      new com.example.demo.modules.notification.dto.SendNotificationRequest();
              
              if (incident.getStatus() == IncidentStatus.RESOLVED) {
                  request.setTitle("🟢 API đã phục hồi: " + incident.getMonitor().getName());
                  request.setContent(String.format("API '%s' (%s) đã hoạt động bình thường trở lại. Status code: %d.",
                          incident.getMonitor().getName(),
                          incident.getMonitor().getUrl(),
                          incident.getLastStatusCode() != null ? incident.getLastStatusCode() : 200));
                  request.setLevel(com.example.demo.modules.notification.enums.NotificationLevel.INFO);
              } else {
                  String severitySymbol = incident.getSeverity() == IncidentSeverity.CRITICAL ? "🔴" : "⚠️";
                  request.setTitle(String.format("%s Cảnh báo API: %s %s",
                          severitySymbol,
                          incident.getMonitor().getName(),
                          incident.getType()));
                  request.setContent(String.format("Phát hiện lỗi tại API '%s' (%s). Trạng thái: %s. Nội dung: %s.",
                          incident.getMonitor().getName(),
                          incident.getMonitor().getUrl(),
                          incident.getSeverity(),
                          incident.getMessage()));
                  request.setLevel(incident.getSeverity() == IncidentSeverity.CRITICAL 
                          ? com.example.demo.modules.notification.enums.NotificationLevel.SYSTEM 
                          : com.example.demo.modules.notification.enums.NotificationLevel.WARNING);
              }

              request.setTargetType(com.example.demo.modules.notification.enums.TargetType.SINGLE);
              request.setTargetValue(userEmail);
              request.setSendWeb(true);
              request.setSendEmail(false); // Đã có Email strategy xử lý riêng

              notificationService.sendNotification(request);
              log.info("[IncidentService] Triggered web notification for userId={}", userId);
          } catch (Exception e) {
              log.error("[IncidentService] Failed to send web notification: {}", e.getMessage());
          }
      }
  ```

- [ ] **Step 3: Tích hợp gọi hàm thông báo trong `processCheckResult` và `resolveActiveIncidents`**
  
  * Trong `processCheckResult` cạnh vị trí `notificationDispatcher.dispatch(saved)`:
    ```java
    // Gọi triggerWebNotification(saved);
    ```
  * Trong `resolveActiveIncidents` cạnh vị trí `notificationDispatcher.dispatch(i)`:
    ```java
    // Gọi triggerWebNotification(i);
    ```
  *(Lưu ý: Phải gọi thông qua Transaction synchronization sau khi commit thành công).*

- [ ] **Step 4: Commit thay đổi**
  ```bash
  git add src/main/java/com/example/demo/modules/alert/services/IncidentService.java
  git commit -m "feat: trigger web notification on incident down/warning/recovery"
  ```

---

### Task 5: Viết Unit Test và chạy kiểm thử tự động

**Files:**
* Create: `src/test/java/com/example/demo/modules/notification/services/NotificationSseServiceTest.java`

- [ ] **Step 1: Tạo Unit Test kiểm tra cơ chế gửi tin và Pub/Sub**
  
  Tạo file `src/test/java/com/example/demo/modules/notification/services/NotificationSseServiceTest.java` như sau:
  ```java
  package com.example.demo.modules.notification.services;

  import com.example.demo.modules.notification.dto.SseNotificationPayload;
  import com.example.demo.modules.notification.dto.UserNotificationResponse;
  import com.example.demo.common.config.RedisPubSubConfig;
  import org.junit.jupiter.api.BeforeEach;
  import org.junit.jupiter.api.Test;
  import org.junit.jupiter.api.extension.ExtendWith;
  import org.mockito.ArgumentCaptor;
  import org.mockito.Mock;
  import org.mockito.junit.jupiter.MockitoExtension;
  import org.springframework.data.redis.core.RedisTemplate;
  import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

  import java.lang.reflect.Field;
  import java.util.Map;
  import java.util.UUID;

  import static org.junit.jupiter.api.Assertions.assertEquals;
  import static org.junit.jupiter.api.Assertions.assertNotNull;
  import static org.mockito.ArgumentMatchers.eq;
  import static org.mockito.Mockito.*;

  @ExtendWith(MockitoExtension.class)
  public class NotificationSseServiceTest {

      @Mock
      private RedisTemplate<String, Object> redisTemplate;

      private NotificationSseServiceImpl sseService;

      @BeforeEach
      void setUp() {
          sseService = new NotificationSseServiceImpl(redisTemplate);
      }

      @Test
      void testSendNotification_PublishesToRedis() {
          // Arrange
          UUID userId = UUID.randomUUID();
          UserNotificationResponse response = UserNotificationResponse.builder()
                  .title("Test alert")
                  .content("API Down")
                  .build();

          // Act
          sseService.sendNotification(userId, response);

          // Assert
          ArgumentCaptor<SseNotificationPayload> payloadCaptor = ArgumentCaptor.forClass(SseNotificationPayload.class);
          verify(redisTemplate, times(1)).convertAndSend(
                  eq(RedisPubSubConfig.SSE_CHANNEL), payloadCaptor.capture());
          
          SseNotificationPayload captured = payloadCaptor.getValue();
          assertEquals(userId, captured.getUserId());
          assertEquals("Test alert", captured.getNotification().getTitle());
      }

      @Test
      @SuppressWarnings("unchecked")
      void testSendNotificationLocal_DeliversToActiveEmitter() throws Exception {
          // Arrange
          UUID userId = UUID.randomUUID();
          UserNotificationResponse response = UserNotificationResponse.builder()
                  .title("Local alert")
                  .content("API recovered")
                  .build();

          // mock emitter
          SseEmitter emitter = mock(SseEmitter.class);
          
          // inject emitter to service emitters map via reflection
          Field emittersField = NotificationSseServiceImpl.class.getDeclaredField("emitters");
          emittersField.setAccessible(true);
          Map<UUID, SseEmitter> emitters = (Map<UUID, SseEmitter>) emittersField.get(sseService);
          emitters.put(userId, emitter);

          // Act
          sseService.sendNotificationLocal(userId, response);

          // Assert
          verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
      }
  }
  ```

- [ ] **Step 2: Chạy kiểm thử tự động để xác nhận mọi test case đều Pass**
  
  Run: `mvn test -Dtest=NotificationSseServiceTest`
  Expected: BUILD SUCCESS (Các test cases đều vượt qua thành công)

- [ ] **Step 3: Commit và báo cáo hoàn thành**
  ```bash
  git add src/test/java/com/example/demo/modules/notification/services/NotificationSseServiceTest.java
  git commit -m "test: add unit tests for SSE notification service and Redis pub/sub integration"
  ```
