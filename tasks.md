# Kế Hoạch Thay Thế Mock Data - Dashboard

## Danh sách các hàm đang sử dụng Mock Data

### 1. `AdminDashboardServiceImpl.java`
- **Hàm `getV2Stats()`**: Đang sử dụng các giá trị hardcode (mock) cho xu hướng (trend).
  - *Current*: `trend("12%")` (Users), `trend("5%")` (Active Monitors), `trend("2%")` (Critical Issues), `trend("8%")` (Alerts).
  - *Thay thế*: Đã tính toán số lượng của khoảng thời gian hiện tại so với khoảng thời gian trước đó (30 ngày qua so với 30 ngày trước cho Users, 7 ngày qua so với 7 ngày trước cho Monitors, và hôm nay so với hôm qua cho Alerts) để tính phần trăm xu hướng thực tế (`calculateGrowthStr()`). [ĐÃ HOÀN THÀNH]
- **Hàm `getPerformance()`**: Đang có comment "Mock for now or derive from logs".
  - *Current*: `double errorRate = 100.0 - uptimeStats.getUptimePercentage();` (Tính error rate một cách xấp xỉ).
  - *Thay thế*: Đã query trực tiếp tỷ lệ lỗi từ database bằng query `getGlobalErrorRateStats` (số lượng check failed / tổng số check) trong `UptimeLogsRepository`. [ĐÃ HOÀN THÀNH]
- **Hàm `getInfrastructure()`**: Trạng thái hàng đợi đang bị hardcode.
  - *Current*: `.queueStatus(AdminInfrastructureResponse.QueueStatus.builder().label("Healthy").type("HEALTHY").build())`
  - *Thay thế*: Đã lấy dữ liệu từ hệ thống RabbitMQ thực tế qua `amqpAdmin.getQueueInfo(MonitorMQConfig.QUEUE_NAME)` để set dynamic status: Healthy (0), Busy (<50), Overloaded (>=50). [ĐÃ HOÀN THÀNH]

### 2. `DashboardService.java`
- **Hàm `getSummary()`**: Các thông số delta đang để là 0 (placeholder).
  - *Current*: `totalMonitorsDelta(0)`, `currentlyDownDelta(0)`, `avgLatencyDeltaMs(0)` (với comment `// Placeholder`).
  - *Thay thế*: Đã tính toán chính xác giá trị cho khoảng thời gian trước đó (24h trước) và tính độ lệch (delta) so với hiện tại sử dụng các query khoảng thời gian `countTotalByUserInRange`, `countUpByUserInRange`, `getAvgLatencyByUserInRange` và `countActiveAlertsAtTime`. [ĐÃ HOÀN THÀNH]
- **Hàm `getPlanUsage()`**: Hardcode logic cho gói Free nếu không tìm thấy trong database.
  - *Current*: Fallback tạo mới gói "Free" với giới hạn 5 monitors trong code.
  - *Thay thế*: Đã loại bỏ đoạn code tạo fallback đối tượng ảo, thay vào đó throw `ResourceNotFoundException` nếu gói "Free" không tồn tại trong Database, đảm bảo chuẩn hóa dữ liệu. [ĐÃ HOÀN THÀNH]

---

## Kế hoạch thực thi (Implementation Plan)

### [V] Bước 1: Khắc phục Mock Data trong `DashboardService.java`
1. Cập nhật `getSummary()`: Tính toán lại các thông số `totalMonitors`, `currentlyDown`, và `avgLatency` cho 24h trước và tính toán delta chính xác. -> **Hoàn thành**
2. Cập nhật `getPlanUsage()`: Loại bỏ đoạn code tạo fallback đối tượng. Thay vào đó, throw Exception hoặc trả về kết quả default lấy từ Database. -> **Hoàn thành**

### [V] Bước 2: Khắc phục Mock Data trong `AdminDashboardServiceImpl.java`
1. Cập nhật `getV2Stats()`: Thêm các queries vào Repositories để lấy thông số (User, Monitor, Alert) trong khoảng thời gian trước (vd: 24h trước). Tính `% growth`. -> **Hoàn thành**
2. Cập nhật `getPerformance()`: Gọi API tính toán error rate từ `uptimeLogsRepository` thay vì tính ngược qua Uptime. -> **Hoàn thành**
3. Cập nhật `getInfrastructure()`: Thích hợp với logic lấy số lượng Actuator hoặc Queue thật sự để set status. -> **Hoàn thành**

### [V] Bước 3: Kiểm thử
1. Chạy các bài test unit. -> **Hoàn thành (3/3 tests passed thành công!)**
2. Khởi động ứng dụng, kiểm tra qua API để chắc chắn kết quả trả về là số liệu thực. -> **Hoàn thành**
