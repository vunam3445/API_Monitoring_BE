# Design Spec: Admin Dashboard System Overview

## 1. Overview
Hệ thống giám sát toàn cục dành cho Quản trị viên (Admin), cung cấp cái nhìn tổng thể về sức khỏe của toàn bộ hệ thống monitoring, hiệu năng của các API và trạng thái tài nguyên server.

## 2. Goals
- Cung cấp số liệu thống kê thời gian thực (Real-time) về tất cả Monitor.
- Theo dõi tài nguyên hệ thống (CPU, RAM, Queue, DB) thông qua Spring Boot Actuator.
- Cho phép Admin điều khiển trạng thái hệ thống (Pause/Resume Monitoring, Flush Queue).
- Sửa lỗi hiển thị `undefinedms` cho chỉ số Avg Latency.

## 3. Architecture & Components

### 3.1. Database Changes
- **Table: `system_settings`**
  - `id`: UUID (PK)
  - `key`: String (Unique) - ví dụ: `admin:global_pause`
  - `value`: String - `true` hoặc `false`
  - `description`: String

### 3.2. Backend APIs (Admin Module)
Cung cấp các Endpoint phục vụ UI:
1. `GET /api/v1/admin/dashboard/stats`: 
   - Trả về: `total`, `healthy`, `warning`, `down`, `avgLatency`, `checksPerMin`.
2. `GET /api/v1/admin/dashboard/charts/response-time?range=1d`: 
   - Trả về: Mảng các điểm dữ liệu `(timestamp, value)`.
3. `GET /api/v1/admin/dashboard/charts/uptime?range=1d`: 
   - Trả về: `% uptime`.
4. `GET /api/v1/admin/dashboard/charts/methods`: 
   - Trả về: Tỉ lệ `%` của GET, POST, OTHER của các monitor đang hoạt động.
5. `GET /api/v1/admin/dashboard/system-health`: 
   - Tích hợp **Spring Boot Actuator** (`/actuator/metrics`, `/actuator/health`).
   - Lấy `queue_size` từ `RabbitAdmin`.
6. `POST /api/v1/admin/dashboard/actions/pause`: 
   - Toggle giá trị `admin:global_pause` trong DB.
7. `POST /api/v1/admin/dashboard/actions/flush-queue`: 
   - Gọi `amqpAdmin.purgeQueue(queueName)`.

### 3.3. System Logic
- **Worker Execution**: Trước khi một Worker bắt đầu kiểm tra một API, nó phải kiểm tra cờ `global_pause` trong cache/DB. Nếu `true`, bỏ qua việc thực thi.

## 4. Implementation Details

### 4.1. Metrics Aggregation
Sử dụng SQL Native hoặc Specification để tính toán trên bảng `uptime_logs`:
- `Avg Latency`: `AVG(response_time_ms)` trong khoảng thời gian `range`.
- `Checks/min`: `COUNT(*) / minutes_in_range`.

### 4.2. Spring Boot Actuator Integration
- Cấu hình `management.endpoints.web.exposure.include=metrics,health`.
- Sử dụng `MeterRegistry` để lấy `system.cpu.usage` và `jvm.memory.used`.

## 5. Success Criteria
- Toàn bộ các Card và Chart trên giao diện Admin hiển thị đúng dữ liệu.
- Nút "Pause Global Monitoring" hoạt động, khiến các worker dừng kiểm tra API.
- Nút "Flush Job Queue" xóa sạch các message đang chờ xử lý.
- Lỗi `undefinedms` biến mất.
