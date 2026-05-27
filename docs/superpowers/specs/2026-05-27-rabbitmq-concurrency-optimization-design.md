# Tài liệu Thiết kế: Tối ưu hóa RabbitMQ Concurrency & Quản lý Tài nguyên Hệ thống

> [!NOTE]
> Tài liệu này mô tả chi tiết thiết kế tối ưu hóa phân bổ luồng (thread allocation/concurrency) cho toàn bộ hệ thống xử lý bất đồng bộ qua RabbitMQ trong dự án API Monitoring. Thiết kế này giải quyết bài toán lãng phí tài nguyên và rủi ro cạn kiệt Connection Pool của Database.

---

## 1. Bối cảnh & Vấn đề hiện tại

Hiện tại, trong tệp cấu hình `application.properties`, hệ thống đang áp dụng một cấu hình mặc định toàn cục cho toàn bộ RabbitMQ Listeners:
*   `spring.rabbitmq.listener.simple.concurrency = 20`
*   `spring.rabbitmq.listener.simple.max-concurrency = 50`

Điều này dẫn đến việc **tất cả** 4 listener hiện có trong hệ thống khi khởi chạy đều khởi tạo tối thiểu 20 threads độc lập chạy ngầm.

### Các vấn đề nghiêm trọng gặp phải:
1.  **Lãng phí bộ nhớ JVM nhàn rỗi:** Các tác vụ chạy theo lịch trình hàng ngày hoặc tần suất rất thấp (gửi mail, thông báo) chiếm dụng $20 \times 3 = 60$ threads nhàn rỗi liên tục 24/7, tốn RAM và tăng tải quản lý luồng của OS (Thread Context Switching).
2.  **Nguy cơ cạn kiệt Database Connection Pool (HikariCP):** Khi các queue bị dồn ứ tin nhắn, số thread có thể tăng đột biến lên 50 threads cho *mỗi* listener. Trong khi đó, database pool tối đa chỉ là 50 (`maximum-pool-size=50`). Điều này chắc chắn dẫn đến lỗi nghẽn cổ chai DB Connection, gây sập dịch vụ đối với người dùng trực tiếp.
3.  **Lỗi giới hạn SMTP (Brevo limit):** Gửi quá nhiều email đồng thời từ 20 workers có thể kích hoạt cơ chế throttling hoặc chặn kết nối từ máy chủ SMTP Brevo.

---

## 2. Phân tích Đặc thù của 4 Listeners

Để tối ưu hóa một cách khoa học, chúng ta phân loại 4 listeners hiện tại dựa trên tính chất luồng xử lý:

| Tên Queue / Listener | Bản chất Công việc | Tần suất Hoạt động | Phân loại Tác vụ | Đề xuất Luồng (`Min - Max`) |
| :--- | :--- | :--- | :--- | :---: |
| **`MonitorWorker`**<br>(`monitor.execution.queue`) | Kiểm tra API Uptime (Ping API bên thứ 3) | **Cực cao & Liên tục** | Heavy I/O-Bound (Chờ phản hồi mạng) | **`5 - 20`** |
| **`ApplicationLogConsumer`**<br>(`system.logs.queue`) | Đọc logs hệ thống và lưu vào DB | **Trung bình & Liên tục** | Write-Heavy (Ghi DB liên tục) | **`2 - 4`** |
| **`SubscriptionExpiryConsumer`**<br>(`subscription.expiry.queue`) | Gửi email nhắc nhở hết hạn gói | **Rất thấp** (Hàng ngày lúc 7:00 AM) | Bursty SMTP I/O-Bound | **`1 - 2`** |
| **`NotificationBroadcastConsumer`**<br>(`notification.broadcast.queue`) | Phát thông báo web/email thời gian thực | **Thấp / Đột biến** (Khi có sự kiện Admin hoặc User) | Spiky Web SSE + SMTP I/O-Bound | **`1 - 3`** |

---

## 3. Kiến trúc Cấu hình Đề xuất (Enterprise Configurable Pattern)

Để đảm bảo tính linh hoạt, dễ dàng mở rộng và bảo trì theo nguyên lý **SOLID (Open/Closed Principle)** và tiêu chuẩn **12-Factor App**, toàn bộ cấu hình thread sẽ được đưa ra tệp `application.properties` và tham chiếu động vào code Java bằng Property Placeholders.

```mermaid
graph TD
    subgraph Properties File [application.properties]
        global[Global Default: 2-5 threads]
        p1[app.rabbitmq.concurrency.monitor = 5-20]
        p2[app.rabbitmq.concurrency.log = 2-4]
        p3[app.rabbitmq.concurrency.expiry = 1-2]
        p4[app.rabbitmq.concurrency.broadcast = 1-3]
    end

    subgraph Spring Container [Java Consumers Class]
        Listener1[@RabbitListener: MonitorWorker]
        Listener2[@RabbitListener: ApplicationLogConsumer]
        Listener3[@RabbitListener: SubscriptionExpiryConsumer]
        Listener4[@RabbitListener: NotificationBroadcastConsumer]
    end

    p1 -->|inject| Listener1
    p2 -->|inject| Listener2
    p3 -->|inject| Listener3
    p4 -->|inject| Listener4
```

---

## 4. Đặc tả Cấu hình chi tiết

### 4.1. File `application.properties`

```properties
# ==========================================
# ENTERPRISE RABBITMQ CONCURRENCY TUNING
# ==========================================
# Cấu hình mặc định toàn cục cho các queue phát sinh sau này (an toàn & tiết kiệm)
spring.rabbitmq.listener.simple.concurrency=2
spring.rabbitmq.listener.simple.max-concurrency=5
spring.rabbitmq.listener.simple.prefetch=5

# Cấu hình luồng tối ưu hóa riêng biệt theo đặc thù nghiệp vụ
app.rabbitmq.concurrency.monitor=5-20
app.rabbitmq.concurrency.log=2-4
app.rabbitmq.concurrency.expiry=1-2
app.rabbitmq.concurrency.broadcast=1-3
```

### 4.2. Khai báo thuộc tính trong mã nguồn Java

```java
// 1. File MonitorWorker.java
@RabbitListener(
    id = "monitorWorkerContainer", 
    queues = MonitorMQConfig.QUEUE_NAME,
    concurrency = "${app.rabbitmq.concurrency.monitor}"
)

// 2. File ApplicationLogConsumer.java
@RabbitListener(
    queues = "system.logs.queue",
    concurrency = "${app.rabbitmq.concurrency.log}"
)

// 3. File SubscriptionExpiryConsumer.java
@RabbitListener(
    queues = SubscriptionExpiryMQConfig.EXPIRY_QUEUE,
    concurrency = "${app.rabbitmq.concurrency.expiry}"
)

// 4. File NotificationBroadcastConsumer.java
@RabbitListener(
    queues = NotificationMQConfig.BROADCAST_QUEUE,
    concurrency = "${app.rabbitmq.concurrency.broadcast}"
)
```

---

## 5. Đánh giá Chỉ số An toàn & Tối ưu hóa Tài nguyên

### 5.1. Database Connection Pool Safety (HikariCP)
*   **Cấu hình tối đa của HikariCP:** 50 connections.
*   **Kịch bản xấu nhất (Tất cả 4 queue quá tải đồng thời):**
    $$\text{Max Connections Used} = 20\text{ (Monitor)} + 4\text{ (Log)} + 2\text{ (Expiry)} + 3\text{ (Broadcast)} = 29\text{ connections.}$$
*   **Dự phòng an toàn:** Hệ thống luôn đảm bảo dư thừa ít nhất **21 connections** cho máy chủ Web Tomcat để xử lý các truy vấn HTTP trực tiếp từ trình duyệt của người dùng. Hệ thống **hoàn toàn loại bỏ** được rủi ro nghẽn DB Pool.

### 5.2. Hiệu quả tiết kiệm luồng JVM (RAM & CPU)
*   **Khi hệ thống nhàn rỗi (Trạng thái bình thường):**
    $$\text{Min active threads} = 5\text{ (Monitor)} + 2\text{ (Log)} + 1\text{ (Expiry)} + 1\text{ (Broadcast)} = 9\text{ threads.}$$
*   **So sánh hiệu quả:**
    *   *Hệ thống cũ:* Luôn duy trì cố định **80 threads** chạy thường trực.
    *   *Hệ thống mới:* Chỉ cần duy trì tối thiểu **9 threads** và chỉ tự động phình lên khi có tải thực sự.
    *   *Hiệu quả:* **Tiết kiệm tới ~88% số lượng luồng nhàn rỗi**, giải phóng bộ nhớ Stack cho JVM và giảm tải CPU context switching đáng kể.

---

## 6. Kế hoạch Kiểm thử & Xác thực (Verification Plan)

Sau khi triển khai mã nguồn, chúng ta sẽ thực hiện các bước kiểm tra sau để xác nhận cấu hình hoạt động chính xác:
1.  **Khởi động Server:** Đảm bảo Spring Boot context khởi chạy thành công mà không có lỗi cú pháp hoặc lỗi parse placeholder `${...}`.
2.  **Kiểm tra JConsole / Actuator Endpoints:** Truy cập endpoints quản lý luồng để xác nhận số lượng threads khởi động tương ứng đúng với thiết lập tối thiểu (9 threads cho cả 4 listeners).
3.  **Kiểm tra logs hoạt động:** Đảm bảo các tiến trình gửi email, ghi log và ping API vẫn diễn ra mượt mà và không có lỗi gián đoạn.
