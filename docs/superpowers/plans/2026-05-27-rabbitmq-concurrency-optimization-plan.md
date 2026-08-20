# Kế hoạch Triển khai: Tối ưu hóa RabbitMQ Concurrency & Quản lý Tài nguyên

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Triển khai cơ chế co giãn thread thông minh (dynamic concurrency) cho toàn bộ 4 RabbitMQ listeners trong ứng dụng API Monitoring, giúp hệ thống tiết kiệm tài nguyên bộ nhớ JVM nhàn rỗi và loại bỏ hoàn toàn nguy cơ cạn kiệt Database Connection Pool.

**Architecture:** Sử dụng Enterprise Configurable Pattern, tách biệt toàn bộ cấu hình thread số lượng luồng (concurrency) ra tệp cấu hình `application.properties` dưới dạng các tham số riêng biệt theo đặc thù tác vụ, sau đó sử dụng cơ chế Spring Property Placeholder (`${...}`) để tự động nạp động vào các annotation `@RabbitListener` trong các file Java Consumer. Đảm bảo tuân thủ nguyên lý SOLID (Open/Closed Principle) và 12-Factor App.

**Tech Stack:** Java 17, Spring Boot, Spring AMQP (RabbitMQ), JUnit 5, AssertJ.

---

### File Structure Changes

Các tệp tin sẽ được tạo mới hoặc sửa đổi:
- **Tạo mới:** `src/test/java/com/example/demo/modules/subscription/services/RabbitMQConcurrencyConfigTest.java` (Test case để kiểm tra tính năng tiêm thuộc tính).
- **Sửa đổi:**
  - `src/main/resources/application.properties` (Khai báo cấu hình thread).
  - `src/main/java/com/example/demo/modules/monitor/workers/MonitorWorker.java` (Cấu hình luồng Monitor API).
  - `src/main/java/com/example/demo/modules/system/services/ApplicationLogConsumer.java` (Cấu hình luồng ghi log).
  - `src/main/java/com/example/demo/modules/subscription/services/SubscriptionExpiryConsumer.java` (Cấu hình luồng mail hết hạn).
  - `src/main/java/com/example/demo/modules/notification/services/NotificationBroadcastConsumer.java` (Cấu hình luồng phát thông báo).

---

### Task 1: Khai báo Cấu hình Properties & Viết Test TDD xác thực

**Files:**
- Create: `src/test/java/com/example/demo/modules/subscription/services/RabbitMQConcurrencyConfigTest.java`
- Modify: `src/main/resources/application.properties`

- [ ] **Step 1: Viết test case TDD kiểm tra cơ chế tiêm thuộc tính cấu hình**
  Tạo tệp `src/test/java/com/example/demo/modules/subscription/services/RabbitMQConcurrencyConfigTest.java` với nội dung kiểm tra xem Spring Context có nạp đúng các giá trị cấu hình luồng từ properties hay không:

  ```java
  package com.example.demo.modules.subscription.services;

  import org.junit.jupiter.api.Test;
  import org.springframework.beans.factory.annotation.Value;
  import org.springframework.boot.test.context.SpringBootTest;
  import static org.assertj.core.api.Assertions.assertThat;

  @SpringBootTest
  public class RabbitMQConcurrencyConfigTest {

      @Value("${app.rabbitmq.concurrency.monitor}")
      private String monitorConcurrency;

      @Value("${app.rabbitmq.concurrency.log}")
      private String logConcurrency;

      @Value("${app.rabbitmq.concurrency.expiry}")
      private String expiryConcurrency;

      @Value("${app.rabbitmq.concurrency.broadcast}")
      private String broadcastConcurrency;

      @Test
      public void testConcurrencyPropertiesAreInjected() {
          assertThat(monitorConcurrency).isEqualTo("5-20");
          assertThat(logConcurrency).isEqualTo("2-4");
          assertThat(expiryConcurrency).isEqualTo("1-2");
          assertThat(broadcastConcurrency).isEqualTo("1-3");
      }
  }
  ```

- [ ] **Step 2: Chạy test và xác nhận test thất bại**
  Chạy lệnh test thông qua Maven để xác minh lỗi do thuộc tính chưa được khai báo:
  *   Run: `mvn test -Dtest=RabbitMQConcurrencyConfigTest`
  *   Expected: **FAIL** (Spring Boot không thể khởi tạo Context vì thiếu các thuộc tính placeholder).

- [ ] **Step 3: Khai báo các biến cấu hình luồng vào `application.properties`**
  Mở [application.properties](file:///e:/API%20Monitoring/src/main/resources/application.properties) và thay thế phần cấu hình cũ tại dòng 91-93 bằng:

  ```properties
  # ==========================================
  # PERFORMANCE OPTIMIZATION & CONCURRENCY
  # ==========================================
  # Tăng số kết nối tối đa xuống Database (HikariCP)
  spring.datasource.hikari.maximum-pool-size=50
  spring.datasource.hikari.minimum-idle=10

  # Cấu hình mặc định toàn cục cho các queue phát sinh sau này
  spring.rabbitmq.listener.simple.concurrency=2
  spring.rabbitmq.listener.simple.max-concurrency=5
  spring.rabbitmq.listener.simple.prefetch=5

  # Định nghĩa cấu hình luồng tối ưu riêng biệt cho từng loại nghiệp vụ
  app.rabbitmq.concurrency.monitor=5-20
  app.rabbitmq.concurrency.log=2-4
  app.rabbitmq.concurrency.expiry=1-2
  app.rabbitmq.concurrency.broadcast=1-3
  ```

- [ ] **Step 4: Chạy lại test để xác nhận đã vượt qua thành công**
  *   Run: `mvn test -Dtest=RabbitMQConcurrencyConfigTest`
  *   Expected: **PASS** (Spring Boot nạp thành công các giá trị vào JUnit test).

- [ ] **Step 5: Thực hiện Commit**
  *   Command:
      ```bash
      git add src/test/java/com/example/demo/modules/subscription/services/RabbitMQConcurrencyConfigTest.java src/main/resources/application.properties
      git commit -m "feat(config): add customizable rabbitmq concurrency properties and integration test"
      ```

---

### Task 2: Áp dụng Cấu hình Luồng Tải Cao cho `MonitorWorker`

**Files:**
- Modify: `src/main/java/com/example/demo/modules/monitor/workers/MonitorWorker.java`

- [ ] **Step 1: Áp dụng Property Placeholder vào `@RabbitListener`**
  Mở [MonitorWorker.java](file:///e:/API%20Monitoring/src/main/java/com/example/demo/modules/monitor/workers/MonitorWorker.java) và cập nhật annotation `@RabbitListener` tại dòng 56:

  ```diff
  -    @RabbitListener(id = "monitorWorkerContainer", queues = MonitorMQConfig.QUEUE_NAME)
  +    @RabbitListener(
  +            id = "monitorWorkerContainer", 
  +            queues = MonitorMQConfig.QUEUE_NAME,
  +            concurrency = "${app.rabbitmq.concurrency.monitor}"
  +    )
       public void processMonitorJob(MonitorExecutionMessage message) {
  ```

- [ ] **Step 2: Chạy kiểm thử kiểm tra tính chính xác của cú pháp**
  *   Run: `mvn test -Dtest=RabbitMQConcurrencyConfigTest`
  *   Expected: **PASS** (Đảm bảo thay đổi cú pháp tiêm thuộc tính trên MonitorWorker hoạt động mượt mà không gây lỗi compile/boot).

- [ ] **Step 3: Thực hiện Commit**
  *   Command:
      ```bash
      git add src/main/java/com/example/demo/modules/monitor/workers/MonitorWorker.java
      git commit -m "feat(monitor): configure dynamic high concurrency for MonitorWorker"
      ```

---

### Task 3: Áp dụng Cấu hình Luồng Ghi Log cho `ApplicationLogConsumer`

**Files:**
- Modify: `src/main/java/com/example/demo/modules/system/services/ApplicationLogConsumer.java`

- [ ] **Step 1: Áp dụng Property Placeholder vào `@RabbitListener`**
  Mở [ApplicationLogConsumer.java](file:///e:/API%20Monitoring/src/main/java/com/example/demo/modules/system/services/ApplicationLogConsumer.java) và cập nhật annotation `@RabbitListener` tại dòng 33:

  ```diff
  -    @RabbitListener(queues = "system.logs.queue")
  +    @RabbitListener(
  +            queues = "system.logs.queue",
  +            concurrency = "${app.rabbitmq.concurrency.log}"
  +    )
       public void consumeApplicationLog(Message message) {
  ```

- [ ] **Step 2: Chạy kiểm thử kiểm tra tính chính xác của cú pháp**
  *   Run: `mvn test -Dtest=RabbitMQConcurrencyConfigTest`
  *   Expected: **PASS** (Spring Context khởi chạy thành công).

- [ ] **Step 3: Thực hiện Commit**
  *   Command:
      ```bash
      git add src/main/java/com/example/demo/modules/system/services/ApplicationLogConsumer.java
      git commit -m "feat(system): configure dynamic log concurrency for ApplicationLogConsumer"
      ```

---

### Task 4: Áp dụng Cấu hình Luồng Email Hết Hạn cho `SubscriptionExpiryConsumer`

**Files:**
- Modify: `src/main/java/com/example/demo/modules/subscription/services/SubscriptionExpiryConsumer.java`

- [ ] **Step 1: Áp dụng Property Placeholder vào `@RabbitListener`**
  Mở [SubscriptionExpiryConsumer.java](file:///e:/API%20Monitoring/src/main/java/com/example/demo/modules/subscription/services/SubscriptionExpiryConsumer.java) và cập nhật annotation `@RabbitListener` tại dòng 18:

  ```diff
  -    @RabbitListener(queues = SubscriptionExpiryMQConfig.EXPIRY_QUEUE)
  +    @RabbitListener(
  +            queues = SubscriptionExpiryMQConfig.EXPIRY_QUEUE,
  +            concurrency = "${app.rabbitmq.concurrency.expiry}"
  +    )
       public void consumeExpiryEvent(SubscriptionExpiryEvent event) {
  ```

- [ ] **Step 2: Chạy các test case liên quan đến gia hạn và kiểm tra**
  *   Run: `mvn test -Dtest=SubscriptionExpirySchedulerTest`
  *   Expected: **PASS** (Đảm bảo việc thay đổi cấu hình luồng không làm ảnh hưởng đến tiến trình scheduler gia hạn).

- [ ] **Step 3: Thực hiện Commit**
  *   Command:
      ```bash
      git add src/main/java/com/example/demo/modules/subscription/services/SubscriptionExpiryConsumer.java
      git commit -m "feat(subscription): configure dynamic safe concurrency for SubscriptionExpiryConsumer"
      ```

---

### Task 5: Áp dụng Cấu hình Luồng Phát Thông báo cho `NotificationBroadcastConsumer`

**Files:**
- Modify: `src/main/java/com/example/demo/modules/notification/services/NotificationBroadcastConsumer.java`

- [ ] **Step 1: Áp dụng Property Placeholder vào `@RabbitListener`**
  Mở [NotificationBroadcastConsumer.java](file:///e:/API%20Monitoring/src/main/java/com/example/demo/modules/notification/services/NotificationBroadcastConsumer.java) và cập nhật annotation `@RabbitListener` tại dòng 40:

  ```diff
  -    @RabbitListener(queues = NotificationMQConfig.BROADCAST_QUEUE)
  +    @RabbitListener(
  +            queues = NotificationMQConfig.BROADCAST_QUEUE,
  +            concurrency = "${app.rabbitmq.concurrency.broadcast}"
  +    )
       @Transactional
       public void consume(NotificationBroadcastEvent event) {
  ```

- [ ] **Step 2: Chạy toàn bộ các test kiểm thử để xác thực cấu hình**
  *   Run: `mvn test -Dtest=RabbitMQConcurrencyConfigTest`
  *   Expected: **PASS** (Mọi thành phần khởi tạo Spring hoàn toàn lành mạnh và xanh sạch).

- [ ] **Step 3: Thực hiện Commit**
  *   Command:
      ```bash
      git add src/main/java/com/example/demo/modules/notification/services/NotificationBroadcastConsumer.java
      git commit -m "feat(notification): configure dynamic safe concurrency for NotificationBroadcastConsumer"
      ```
