# 🌐 Hệ Thống Giám Sát API & Cảnh Báo Thời Gian Thực (API Monitoring Platform)

Hệ thống **API Monitoring Platform** là giải pháp giám sát hiệu năng và trạng thái hoạt động của các API Endpoint thời gian thực. Hệ thống hỗ trợ cấu hình đa dạng các loại request, kiểm tra định kỳ tự động, ghi nhận nhật ký uptime chi tiết, phân tích số liệu trực quan và phát thông báo cảnh báo đa kênh (Web, Email, Slack) ngay lập tức khi phát hiện sự cố.

Dự án được phân tách rõ ràng thành hai phần: **Backend (Spring Boot)** và **Frontend (React)**, được thiết kế theo các tiêu chuẩn kỹ thuật hiện đại như Clean Architecture, nguyên lý SOLID, mô hình 12-Factor App và hướng sự kiện (Event-Driven).

---

## 🏛️ 1. Kiến Trúc Hệ Thống & Nguyên Lý Thiết Kế

### 🛡️ Backend (Clean Architecture & SOLID)
Mã nguồn Backend được tổ chức theo mô hình **Clean Architecture** chia thành các tầng độc lập để đảm bảo khả năng mở rộng, bảo trì và kiểm thử dễ dàng:
- **Domain Layer (Entities/Enums):** Chứa các quy tắc nghiệp vụ cốt lõi và thực thể dữ liệu (như `Monitor`, `Incident`, `Subscription`, `User`). Tầng này hoàn toàn độc lập, không phụ thuộc vào framework.
- **Use Cases Layer (Services):** Chứa logic xử lý nghiệp vụ của ứng dụng (ví dụ: kích hoạt giám sát, tính toán doanh thu, gia hạn gói cước).
- **Interface Adapters Layer (Controllers/Repositories/Mappers):** Chuyển đổi dữ liệu giữa tầng nghiệp vụ và thế giới bên ngoài (REST API Endpoints, Spring Data JPA Repositories, MapStruct).
- **Frameworks & Drivers Layer (Config/Security):** Các cấu hình kỹ thuật của hệ thống như Redis, RabbitMQ, Spring Security, AOP Aspects.

### ⚡ Hệ Thống Hướng Sự Kiện & Xử Lý Bất Đồng Bộ
- **Đệm thông điệp với RabbitMQ (AMQP):** Tách biệt tiến trình xử lý I/O nặng (ping API, gửi mail SMTP, ghi log hệ thống) ra khỏi luồng request chính để tối ưu hóa hiệu năng, tăng khả năng chịu tải và khả năng tự phục hồi.
- **Đẩy tin đa Instance với Redis Pub/Sub:** Hỗ trợ mở rộng hệ thống theo chiều ngang (Horizontal Scaling). Khi một node bất kỳ trong cụm server nhận được sự kiện thông báo từ RabbitMQ, nó sẽ phát tán (broadcast) lên Redis Pub/Sub channel. Tất cả các node đang chạy sẽ nhận được tin nhắn và đẩy Server-Sent Events (SSE) trực tiếp xuống các trình duyệt Web đang kết nối cục bộ.

---

## 🛠️ 2. Công Nghệ & Hạ Tầng Sử Dụng (Tech Stack)

### ☕ Backend (E:\API Monitoring)
| Công nghệ / Thư viện | Phiên bản | Vai trò & Ứng dụng |
| :--- | :--- | :--- |
| **Java** | 17 | Ngôn ngữ lập trình chính, sử dụng các tính năng mới như Record, Switch Expression. |
| **Spring Boot** | 4.0.3 | Framework phát triển backend chính (phiên bản Spring Boot 4 thế hệ mới). |
| **Spring Security** | 6.x | Cấu hình bảo mật hệ thống, phân quyền (User/Admin), xác thực JWT & Google OAuth2. |
| **Spring WebClient / WebFlux** | - | Thực thi các cuộc gọi HTTP để kiểm tra (ping) API mục tiêu một cách bất đồng bộ phi chặn (Non-blocking). |
| **PostgreSQL** | - | Cơ sở dữ liệu quan hệ lưu trữ dữ liệu người dùng, monitor, log, hóa đơn. |
| **Redis** | - | Làm bộ đệm Caching (Cache-aside) ngăn chặn Cache Penetration và làm Broker cho Redis Pub/Sub. |
| **Redisson** | 3.40.2 | Cung cấp khóa phân tán (Distributed Lock) giúp ngăn chặn trùng lặp tiến trình kiểm tra API trong môi trường multi-instance. |
| **RabbitMQ** | - | Message Broker điều phối xử lý ngầm (Monitor worker, Application Log, Email sender). |
| **MapStruct** | 1.5.5 | Tự động ánh xạ nhanh chóng và hiệu năng cao giữa Entity và DTO. |
| **SMTP (Brevo)** | - | Dịch vụ gửi email thông báo sự cố, email nhắc nhở gia hạn gói cước. |
| **Cloudinary** | 2.0.0 | Quản lý lưu trữ ảnh đại diện người dùng. |
| **Springboot4-dotenv**| 5.1.0 | Hỗ trợ nạp cấu hình bảo mật từ tệp `.env` theo chuẩn 12-Factor App. |

### ⚛️ Frontend (E:\API Monitoring FE)
| Công nghệ / Thư viện | Phiên bản | Vai trò & Ứng dụng |
| :--- | :--- | :--- |
| **React** | 19.2.0 | Thư viện xây dựng giao diện người dùng Single Page Application (SPA). |
| **Vite** | 7.3.1 | Bundler thế hệ mới giúp tăng tốc độ build và hot reload cực nhanh. |
| **Tailwind CSS** | 4.2.1 | UI Styling framework với hiệu năng tối ưu và hỗ trợ cấu hình hiện đại. |
| **React Router Dom** | 7.13.1 | Quản lý định tuyến trang, phân quyền truy cập giữa User và Admin Panel. |
| **Axios** | 1.13.6 | HTTP Client gửi request lên API Backend. |
| **Event Source Polyfill**| 1.0.31 | Đảm bảo kết nối Server-Sent Events (SSE) thời gian thực ổn định trên mọi trình duyệt. |
| **React OAuth Google** | 0.13.4 | Hỗ trợ đăng nhập một chạm bằng tài khoản Google. |

---

## 🌟 3. Các Tính Năng Nghiệp Vụ Nổi Bật

### 1. Giám Sát API Toàn Diện & Tùy Biến Cao
- Hỗ trợ cấu hình endpoint đa dạng: HTTP Methods (GET, POST, PUT, DELETE...), Custom Headers, Query Parameters, Request Body (JSON) và Authentication (Bearer Token, Basic Auth).
- Thiết lập tần suất kiểm tra linh hoạt (Check Interval) và điều kiện phản hồi thành công (Expected Status Codes, Max Response Time).

### 2. Phòng Chống SSRF & DNS Rebinding Triệt Để
Để bảo vệ hạ tầng máy chủ tránh bị lợi dụng để tấn công nội bộ hoặc các dịch vụ đám mây thông qua chức năng ping API, hệ thống tích hợp **UrlSecurityValidator**:
- **Chống SSRF:** Phân giải DNS động ngay trước khi gọi request (`InetAddress.getAllByName(host)`) và chặn đứng nếu đích đến là dải IP Localhost (`127.0.0.0/8`, `::1`), Wildcard (`0.0.0.0`), Link-Local (`169.254.0.0/16`), Private Network (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`) hoặc IP Metadata Service (`169.254.169.254`).
- **Chống Parameter / CRLF Injection:** Loại bỏ các ký tự điều khiển độc hại (`\r`, `\n`) trong URL/Headers nhằm chặn đứng tấn công HTTP Response Splitting.

### 3. Cảnh Báo & Thông Báo Thời Gian Thực Đa Kênh (SSE, Email, Slack)
- **Thông báo trên Web (Bell Notification):** Sử dụng kết nối Server-Sent Events (SSE) kết hợp với Redis Pub/Sub để phát thông báo tức thời ngay khi API Down/Warning/Recovery cho dù người dùng đang kết nối ở bất kỳ instance server nào.
- **Cảnh báo qua Email & Slack:** Sử dụng mẫu email HTML cao cấp, trình bày trực quan chi tiết lỗi và tích hợp nút liên kết nhanh xử lý sự cố. Hỗ trợ gửi webhook qua Slack.

### 4. Đăng Ký Gói Cước & Thanh Toán Trực Tuyến VNPay
- Quản lý gói cước (Subscription Plan) với các hạn mức số lượng monitor và tần suất ping khác nhau (FREE, PRO, ENTERPRISE...).
- Tích hợp cổng thanh toán **VNPay** hỗ trợ người dùng thanh toán nâng cấp gói cước trực tuyến an toàn.
- **Tự động nhắc gia hạn:** Cron Job chạy lúc 7:00 sáng hàng ngày quét các subscription sắp hết hạn sau 3 ngày, đẩy sự kiện vào RabbitMQ để gửi email nhắc nhở HTML đến người dùng.

### 5. Quản Trị Hệ Thống & Phân Tích (Admin Panel)
- Biểu đồ phân tích doanh thu (Revenue), số lượng monitor hoạt động, tỷ lệ Uptime chung.
- Quản lý danh sách người dùng, phân quyền Admin/User, khóa/mở khóa tài khoản hoặc chặn các monitor vi phạm chính sách.
- Theo dõi log hệ thống (System Logs), log giao dịch (Payment Logs), và nhật ký hoạt động (Uptime Logs) chi tiết.

---

## 📂 4. Cấu Trúc Thư Mục Dự Án

### ☕ Backend Structure
```text
e:\API Monitoring\
├── .agent/                  # Tài liệu cấu hình tác vụ AI
├── docs/
│   └── superpowers/specs/   # Các tài liệu đặc tả thiết kế kỹ thuật (SSE, Security, RabbitMQ...)
├── src/
│   ├── main/
│   │   ├── java/com/example/demo/
│   │   │   ├── common/
│   │   │   │   ├── config/              # Redis, RabbitMQ, Security, WebClient config
│   │   │   │   ├── exceptions/          # Quản lý lỗi tập trung
│   │   │   │   └── security/            # Bảo mật SSRF, Custom Annotations (@SafeUrl)
│   │   │   ├── modules/
│   │   │   │   ├── alert/               # Incident, Slack & Email Notification strategies
│   │   │   │   ├── auth/                # JWT Auth, Google Sign-in, User Registration
│   │   │   │   ├── dashboard/           # API thống kê, phân tích biểu đồ
│   │   │   │   ├── monitor/             # Monitor logic, Workers, DNS validator, Scheduler
│   │   │   │   ├── notification/        # RabbitMQ Config & Consumers, SSE Service, Redis Pub/Sub
│   │   │   │   ├── payment/             # VNPay Integration, IPN Handler
│   │   │   │   ├── subscription/        # Gia hạn, Quản lý gói cước, Hết hạn Scheduler
│   │   │   │   ├── system/              # System logs, Configurations
│   │   │   │   └── user/                # Quản lý thông tin & quyền người dùng
│   │   │   └── ApiMonitoringApplication.java
│   │   └── resources/
│   │       ├── application.properties   # File cấu hình hệ thống chính (DB, Mail, RabbitMQ, Concurrency)
│   │       └── logback-spring.xml       # Cấu hình log đầu ra của server
│   └── test/                        # Hệ thống Unit Tests cho các Module chính
├── pom.xml                  # Cấu hình Maven Dependencies
└── .env.example             # Tệp cấu hình các biến môi trường mẫu
```

### ⚛️ Frontend Structure
```text
E:\API Monitoring FE\
├── public/                  # Các file tĩnh (favicon, logo, icons)
├── src/
│   ├── assets/              # Ảnh minh họa, tài nguyên tĩnh
│   ├── components/          # Components dùng chung (Navbar, Sidebar, Modal, Button...)
│   ├── hooks/               # Custom hooks quản lý trạng thái (useAuth, useSSE...)
│   ├── pages/               # Các trang giao diện chính
│   │   ├── Alerts/          # Hiển thị và xử lý danh sách sự cố
│   │   ├── APIList/         # Thêm/Sửa/Xóa và Danh sách API cần theo dõi
│   │   ├── Billing/         # Xem gói dịch vụ, Lịch sử hóa đơn, Nâng cấp qua VNPay
│   │   ├── Dashboard/       # Biểu đồ thống kê hiệu năng, tỷ lệ Uptime
│   │   ├── Intro/           # Landing page giới thiệu dịch vụ
│   │   ├── Logs/            # Nhật ký Ping API, System Logs
│   │   ├── Monitoring/      # Chi tiết hoạt động và biểu đồ phản hồi của một Monitor
│   │   ├── Payment/         # Trang xử lý kết quả phản hồi VNPay
│   │   └── Settings/        # Thiết lập tài khoản cá nhân, mật khẩu
│   ├── services/            # Axios API Clients gọi lên Backend
│   ├── utils/               # Định dạng ngày tháng, tính toán hạn gói cước
│   ├── App.jsx              # Định tuyến Routes và Cấu hình bọc Provider
│   ├── index.css            # Nạp TailwindCSS v4
│   └── main.jsx             # File khởi tạo React DOM
├── package.json             # Khai báo thư viện & kịch bản chạy
└── vite.config.js           # Cấu hình bundler Vite
```

---

## 🚀 5. Hướng Dẫn Cài Đặt & Chạy Dự Án (Quick Start)

### 📋 Yêu Cầu Hệ Thống (Prerequisites)
- **Java JDK 17** trở lên.
- **Node.js** phiên bản 18 trở lên.
- **Maven** 3.8+.
- Cơ sở dữ liệu **PostgreSQL** đang chạy.
- Máy chủ **Redis** (cổng mặc định `6379`).
- Máy chủ **RabbitMQ** (cổng mặc định `5672`, trang quản lý `15672`).

---

### 📥 Bước 1: Khởi động Hạ tầng (Docker Compose)
Dự án cung cấp sẵn tệp `docker-compose.yml` để khởi chạy nhanh các dịch vụ hạ tầng (PostgreSQL, Redis, RabbitMQ):
```bash
# Tại thư mục gốc Backend
docker-compose up -d
```

---

### ⚙️ Bước 2: Cấu hình biến môi trường
Tạo tệp `.env` tại thư mục gốc của dự án Backend (`e:\API Monitoring\.env`) dựa trên `.env.example`:
```env
# Database
DB_URL=jdbc:postgresql://localhost:5432/API_Monitoring
DB_USERNAME=postgres
DB_PASSWORD=your_password

# JWT & Authentication
JWT_SECRET=your_super_secret_key_at_least_256_bits_long
JWT_EXPIRATION=86400000
GOOGLE_CLIENT_ID=your_google_client_id

# Cloudinary
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret

# VNPay
VNPAY_TMN_CODE=your_tmn_code
VNPAY_HASH_SECRET=your_hash_secret
VNPAY_PAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_RETURN_URL=http://localhost:3000/payment/vnpay-return

# Brevo SMTP Mailer
SMTP_BREVO_USERNAME=your_brevo_smtp_email
SMTP_BREVO_PASSWORD=your_brevo_smtp_password
```

---

### ☕ Bước 3: Build & Chạy Backend
Khởi chạy dịch vụ Spring Boot thông qua Maven wrapper:
```bash
# Cài đặt các dependencies và build dự án
./mvnw clean install

# Khởi chạy dự án
./mvnw spring-boot:run
```
*Mặc định backend sẽ chạy tại địa chỉ: `http://localhost:8080`*

---

### ⚛️ Bước 4: Chạy Frontend
1. Tạo tệp `.env` tại thư mục gốc Frontend (`E:\API Monitoring FE\.env`):
   ```env
   VITE_API_BASE_URL=http://localhost:8080/api/v1
   VITE_GOOGLE_CLIENT_ID=your_google_client_id
   ```
2. Khởi chạy dự án ở chế độ phát triển (Development mode):
   ```bash
   # Di chuyển vào thư mục Frontend
   cd "E:\API Monitoring FE"

   # Cài đặt các thư viện
   npm install

   # Khởi chạy dev server
   npm run dev
   ```
*Mặc định frontend sẽ chạy tại địa chỉ: `http://localhost:3000` (hoặc cổng được Vite chỉ định)*

---

## 🧪 6. Quy Trình Kiểm Thử & Tự Động Xác Minh
Hệ thống cung cấp sẵn các bộ Unit Test và Integration Test kiểm thử tự động cho các luồng nghiệp vụ nhạy cảm:
- **Kiểm thử bảo mật (SSRF & Injection):** Xác minh tính đúng đắn của `UrlSecurityValidator` với các dải IP nội bộ và các ký tự CRLF độc hại.
- **Kiểm thử đa instance (SSE & Redis):** Xác minh luồng dữ liệu truyền tải sự kiện thông báo từ instance này sang instance khác thông qua Redis Pub/Sub.
- **Kiểm thử bất đồng bộ (RabbitMQ):** Xác minh cơ chế xếp hàng và xử lý ngầm hoạt động ổn định kể cả khi mất kết nối cơ sở dữ liệu tạm thời.

Để khởi chạy toàn bộ các bài kiểm thử, sử dụng lệnh:
```bash
./mvnw test
```