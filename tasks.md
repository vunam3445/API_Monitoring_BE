# 📃 ĐẶC TẢ YÊU CẦU TẠO API GIÁM SÁT LOG LỖI HỆ THỐNG (SYSTEM LOGS)

Tài liệu này phân tích chi tiết giao diện giám sát log của Admin (`AdminSystemLogs.jsx`) và định nghĩa các yêu cầu tạo API tương ứng ở Spring Boot Backend để hoàn thiện tính năng hiển thị dữ liệu động (bỏ qua WebSocket).

---

## 🏛️ 1. Danh sách các API cần xây dựng

Hệ thống Spring Boot Backend cần cung cấp 3 API RESTful dưới đây:

| STT | Tên API | Method | Endpoint | Quyền truy cập |
|---|---|---|---|---|
| 1 | Lấy danh sách logs (có phân trang & bộ lọc) | `GET` | `/api/v1/admin/system-logs` | ADMIN |
| 2 | Lấy số liệu thống kê logs trong ngày hôm nay | `GET` | `/api/v1/admin/system-logs/stats` | ADMIN |
| 3 | Thực hiện dọn dẹp các log cũ hơn 30 ngày | `DELETE` | `/api/v1/admin/system-logs/clear` | ADMIN |

---

## ⚙️ 2. Mô tả chi tiết từng API

### 📑 API 1: Lấy danh sách System Logs (Phân trang & Bộ lọc)
API này dùng để tải danh sách logs hiển thị lên bảng Grid chính, hỗ trợ các thao tác tìm kiếm toàn văn, lọc theo Log Level và lọc theo khoảng thời gian xảy ra sự cố.

#### A. Dữ liệu đầu vào (Request Inputs - Query Parameters)
Tất cả các tham số đầu vào đều là tùy chọn (Optional):

| Tên tham số | Kiểu dữ liệu | Giá trị mặc định | Mô tả |
|---|---|---|---|
| `page` | Integer | `0` | Số thứ tự trang cần lấy (0-indexed ở Backend). |
| `size` | Integer | `50` | Số dòng log tối đa trên một trang. |
| `level` | String | `ALL` | Bộ lọc theo cấp độ log. Giá trị chấp nhận: `ALL`, `INFO`, `WARN`, `ERROR`, `FATAL`. |
| `keyword` | String | *Rỗng* | Từ khóa tìm kiếm toàn văn (Backend sẽ tìm kiếm khớp chuỗi không phân biệt hoa/thường trên các cột: `message`, `component`, `threadId`). |
| `timeRange` | String | `ALL` | Lọc nhanh theo khoảng thời gian. Giá trị chấp nhận: `ALL` (Tất cả), `5m` (5 phút trước), `1h` (1 giờ trước), `24h` (24 giờ trước). |

#### B. Dữ liệu đầu ra mẫu (Response Output - JSON)
* **HTTP Status:** `200 OK`
* **Content-Type:** `application/json`
* **Cấu trúc JSON (Chuẩn Spring Data Page):**

```json
{
  "content": [
    {
      "id": "log-1779533601577-124",
      "timestamp": "2026-05-23T19:30:15.124Z",
      "level": "ERROR",
      "component": "com.example.demo.modules.system.services.MailService",
      "threadId": "task-scheduler-2",
      "message": "Failed to connect to SMTP server smtp.gmail.com:465. Connection timed out.",
      "stackTrace": "java.net.ConnectException: Connection timed out\n\tat java.base/sun.nio.ch.Net.connect0(Native Method)\n\tat java.base/sun.nio.ch.Net.connect(Net.java:579)\n\tat com.sun.mail.smtp.SMTPTransport.openServer(SMTPTransport.java:2175)"
    },
    {
      "id": "log-1779533600210-520",
      "timestamp": "2026-05-23T19:29:10.045Z",
      "level": "INFO",
      "component": "com.example.demo.security.JwtAuthenticationFilter",
      "threadId": "http-nio-8080-exec-1",
      "message": "Successfully authenticated user 'admin_user' from IP 192.168.1.105",
      "stackTrace": null
    }
  ],
  "totalPages": 3,
  "totalElements": 150,
  "size": 50,
  "number": 0,
  "numberOfElements": 2,
  "first": true,
  "last": false,
  "empty": false
}
```

---

### 📊 API 2: Số liệu thống kê logs trong ngày hôm nay (Quick Stats)
API này tự động tính toán tổng số logs, số lượng logs INFO, WARNING, ERROR, FATAL phát sinh trong ngày hôm nay (tính từ `00:00:00` đến thời điểm hiện tại) để hiển thị lên 5 thẻ Stats trên cùng của giao diện.

#### A. Dữ liệu đầu vào (Request Inputs)
* Không có tham số đầu vào. API tự động lọc theo múi giờ hệ thống của ngày hiện tại.

#### B. Dữ liệu đầu ra mẫu (Response Output - JSON)
* **HTTP Status:** `200 OK`
* **Content-Type:** `application/json`

```json
{
  "total": 5240,
  "infos": 4850,
  "warnings": 342,
  "errors": 45,
  "fatals": 3
}
```

---

### 🧹 API 3: Thực hiện dọn dẹp các log cũ (Clear Old Logs)
Khi Admin bấm nút "Dọn dẹp log", Frontend sẽ gọi API này để xóa bỏ các log cũ hơn nhằm tránh làm đầy cơ sở dữ liệu. Theo đặc tả nghiệp vụ, API này mặc định xóa các bản ghi log hệ thống cũ hơn 30 ngày.

#### A. Dữ liệu đầu vào (Request Inputs - Query Parameters)
* Mặc định xóa log > 30 ngày. Admin có thể truyền thêm tham số tùy chọn:

| Tên tham số | Kiểu dữ liệu | Giá trị mặc định | Mô tả |
|---|---|---|---|
| `retentionDays` | Integer | `30` | Số ngày giữ lại logs. Các log cũ hơn số ngày này sẽ bị xóa khỏi Database. |

#### B. Dữ liệu đầu ra mẫu (Response Output - JSON)
* **HTTP Status:** `200 OK`
* **Content-Type:** `application/json`

```json
{
  "success": true,
  "message": "Successfully cleared 4210 old log records older than 30 days.",
  "deletedCount": 4210,
  "retentionDays": 30
}
```

---

## 💡 Gợi ý thiết kế logic tối ưu ở Backend
Để tối ưu hóa hiệu năng tối đa cho Spring Boot Backend của dự án **API Monitoring**:
1. **Lọc log tại tầng Logback:** Chỉ cấu hình ghi log cấp độ `WARN`, `ERROR` và `FATAL` vào Database (Table `system_logs`). Các log cấp độ `INFO` thông thường chỉ nên xuất ra Console/File để giảm thiểu 85% số lượng I/O ghi vào DB.
2. **Lập chỉ mục (Index):** Hãy đánh chỉ mục (Index) cho các cột `timestamp`, `level` và `component` trong table database để đảm bảo truy vấn tìm kiếm và lọc logs diễn ra tức thì dưới 50ms ngay cả khi dung lượng bản ghi tăng cao.
