# TASK: Admin Dashboard System Overview Implementation

## 1. Mục tiêu
Hoàn thiện giao diện Admin Dashboard (System Overview) với đầy đủ các số liệu thống kê real-time, biểu đồ xu hướng, và khả năng điều khiển hệ thống toàn cục.

---

## 2. Giai đoạn 1: Hạ tầng & Điều khiển (System Control)
- [x] **Database & Entity**: Tạo bảng `system_settings` để lưu cờ `GLOBAL_MONITORING_PAUSE`.
- [x] **Service Logic**: 
    - Implement `SystemSettingService` để quản lý việc Bật/Tắt chế độ Pause.
    - Cập nhật các Worker (Monitoring Workers) để kiểm tra trạng thái Pause trước khi thực thi job.
- [x] **Queue Management**: Implement API gọi `amqpAdmin.purgeQueue()` để thực hiện tính năng "Flush Job Queue".

---

## 3. Giai đoạn 2: API Thống kê & Biểu đồ (Metrics Aggregation)
- [x] **Summary Stats API**: Viết API `/admin/dashboard/stats` tính toán:
    - Tổng API, số lượng Healthy/Warning/Down.
    - Avg Latency.
    - Checks per minute.
- [x] **Charts API**: Viết các API cho biểu đồ (hỗ trợ tham số `range` mặc định 1 day):
    - `Response Time Trend`: Dữ liệu chuỗi thời gian (Timeseries).
    - `Global Uptime %`: Tỉ lệ uptime hệ thống.
    - `Method Distribution`: Tỉ lệ GET/POST/OTHER của các monitor đang active.
- [x] **Caching**: Áp dụng Redis Cache cho các API thống kê này để tối ưu performance.

---

## 4. Giai đoạn 3: Tích hợp Actuator & System Health
- [ ] **Spring Boot Actuator**: 
    - Cấu hình Actuator để expose các metrics về CPU, RAM, Disk.
    - Viết Service lấy dữ liệu từ Actuator và kết nối Database để trả về cho API `/admin/dashboard/system-health`.
- [ ] **Queue Monitoring**: Lấy số lượng message đang chờ (Pending) từ RabbitMQ Management API hoặc `RabbitAdmin`.

---

## 5. Giai đoạn 4: Frontend Integration & Polish
- [ ] **API Integration**: Kết nối các API mới vào giao diện Admin Dashboard FE.
- [ ] **Real-time Error Rate**: Hiển thị tỉ lệ lỗi thời gian thực dựa trên logs gần nhất.
- [ ] **UI Actions**: 
    - Gắn logic cho nút "Pause Global Monitoring" (với cảnh báo xác nhận).
    - Gắn logic cho nút "Flush Job Queue".
    - Gắn logic cho nút "Refresh Monitoring".
- [ ] **Fix Bug**: Xử lý triệt để lỗi hiển thị `undefinedms` bằng cách map đúng field từ DTO.

---

## 6. Definition of Done
- [ ] Dashboard hiển thị đầy đủ và chính xác dữ liệu như mockup.
- [ ] Chế độ Pause hoạt động trên toàn hệ thống.
- [ ] Flush Queue xóa sạch hàng đợi thành công.
- [ ] Hệ thống hoạt động ổn định, không làm chậm database khi query aggregate.
