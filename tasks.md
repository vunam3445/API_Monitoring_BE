task.md
Backend Tasks - Alerts Module
1. Mục tiêu
Xây dựng backend cho chức năng Alerts để:
tạo incident từ kết quả monitor check
gửi cảnh báo qua Email và Slack Webhook
lưu lịch sử gửi cảnh báo
cung cấp API phục vụ tab Alerts
hỗ trợ summary cards, list, filter, pagination, export
bổ sung Redis caching để giảm tải query và tăng tốc UI
Hệ thống hiện có:
`alert_configs`: cấu hình kênh nhận alert theo monitor
`incidents`: sự cố gốc để render tab Alerts
`alert_deliveries`: log gửi notification theo từng kênh
SMTP Brevo đã cấu hình qua env:
`SMTP_BREVO_HOST=smtp-relay.brevo.com`
`SMTP_BREVO_PORT=587`
`SMTP_BREVO_USERNAME=...`
`SMTP_BREVO_PASSWORD=...`
---
2. Data model đang dùng
2.1. `alert_configs`
Ý nghĩa:
cấu hình kênh nhận cảnh báo cho từng monitor
Field chính:
`monitor_id`
`type`: `EMAIL`, `SLACK`, ...
`destination`
`is_enabled`
2.2. `incidents`
Ý nghĩa:
sự cố gốc để hiển thị trên tab Alerts
Field chính:
`monitor_id`
`type`: `API_DOWN`, `TIMEOUT`, `SLOW_RESPONSE`, `STATUS_CODE_ERROR`
`severity`: `INFO`, `WARNING`, `CRITICAL`
`status`: `ACTIVE`, `ACKNOWLEDGED`, `RESOLVED`
`title`
`message`
`triggered_at`
`resolved_at`
`last_seen_at`
2.3. `alert_deliveries`
Ý nghĩa:
log gửi notification theo từng kênh
Field chính:
`incident_id`
`alert_config_id`
`channel`
`destination`
`status`: `PENDING`, `SENT`, `FAILED`
`message`
`error_message`
`retry_count`
`sent_at`
---
3. Nghiệp vụ tổng quát
3.1. Khi monitor check thất bại hoặc vượt ngưỡng
Backend phải:
phân tích kết quả check
xác định có tạo incident hay không
nếu chưa có incident active cùng loại thì tạo mới
nếu đã có incident active thì update `last_seen_at`, counter, dữ liệu mới nhất
gửi notification theo các `alert_configs` đang bật
lưu từng lần gửi vào `alert_deliveries`
xóa/invalidate các cache liên quan trong Redis
3.2. Khi monitor hồi phục
Backend phải:
tìm incident đang `ACTIVE` hoặc `ACKNOWLEDGED`
chuyển sang `RESOLVED`
set `resolved_at`
nếu có rule gửi recovery notification thì gửi email/slack
lưu log vào `alert_deliveries`
xóa/invalidate cache summary và list liên quan
---
4. Task code theo lớp
4.1. Entity & Enum
hoàn thiện 3 entity:
`AlertConfig`
`Incident`
`AlertDelivery`
hoàn thiện enum:
`AlertChannelType`
`IncidentType`
`IncidentSeverity`
`IncidentStatus`
`AlertDeliveryStatus`
Yêu cầu
dùng `@Enumerated(EnumType.STRING)`
thêm index cho các field query nhiều:
`incidents.monitor_id`
`incidents.status`
`incidents.severity`
`incidents.triggered_at`
`alert_deliveries.incident_id`
`alert_deliveries.status`
`alert_configs.monitor_id`
`alert_configs.type`
---
4.2. Repository
Tạo repository cho:
`AlertConfigRepository`
`IncidentRepository`
`AlertDeliveryRepository`
Query cần có
`AlertConfigRepository`
tìm tất cả config đang bật theo monitor
tìm config theo monitor + channel
kiểm tra monitor có email/slack config không
`IncidentRepository`
tìm incident active theo monitor + type
tìm incident theo id
filter incidents theo:
search
status
severity
type
time range
count summary:
total alerts
active alerts
critical alerts
resolved alerts
`AlertDeliveryRepository`
lấy lịch sử gửi theo incident
đếm số lần gửi fail
lấy delivery gần nhất theo incident + channel
---
4.3. Incident rule evaluator
Tạo service:
`IncidentRuleEvaluator`
Input
monitor
kết quả check/uptime log
user setting (nếu cần default threshold)
Output
có tạo incident hay không
incident type
severity
title
message
Rule tối thiểu
timeout -> `TIMEOUT`
service unreachable / connection failed -> `API_DOWN`
response time > threshold -> `SLOW_RESPONSE`
status code 5xx hoặc status không mong muốn -> `STATUS_CODE_ERROR`
Severity gợi ý
`API_DOWN` -> `CRITICAL`
`TIMEOUT` -> `WARNING` hoặc `CRITICAL` tùy số lần lặp
`SLOW_RESPONSE` -> `WARNING`
`STATUS_CODE_ERROR` -> `WARNING`
---
4.4. Incident service
Tạo:
`IncidentService`
Nhiệm vụ
tạo incident mới
update incident đang active
resolve incident khi monitor hồi phục
tránh tạo incident trùng
Logic tránh trùng
1 monitor chỉ nên có 1 incident `ACTIVE` hoặc `ACKNOWLEDGED` cho cùng `type`
nếu check tiếp tục fail cùng loại:
không tạo record mới
chỉ update:
`last_seen_at`
`consecutive_fail_count`
`avg_latency_ms`
`last_status_code`
`message`
Redis liên quan
sau khi create/update/resolve incident:
invalidate cache summary
invalidate cache first-page list phổ biến
invalidate cache detail của incident nếu có
---
4.5. Notification dispatcher
Tạo:
`AlertNotificationDispatcher`
Nhiệm vụ
nhận 1 `incident`
load tất cả `alert_configs` đang bật của monitor
gửi notification theo từng config
tạo `alert_deliveries`
Yêu cầu
không viết cứng email/slack trong service
dispatch theo `channel`
dễ mở rộng thêm telegram/webhook/discord sau này
---
4.6. Email sender qua Brevo SMTP
Tạo:
`EmailSenderService`
Cấu hình
Đọc từ env:
`SMTP_BREVO_HOST`
`SMTP_BREVO_PORT`
`SMTP_BREVO_USERNAME`
`SMTP_BREVO_PASSWORD`
Task
cấu hình `JavaMailSender`
gửi mail HTML qua Brevo SMTP
set timeout hợp lý
bắt exception rõ ràng
Subject gợi ý
`[CRITICAL] Authentication Svc is DOWN`
`[WARNING] Payment Gateway slow response`
`[RESOLVED] Authentication Svc recovered`
Nội dung email tối thiểu
Monitor/API name
Endpoint
Incident type
Severity
Message
Triggered time
Link tới dashboard nếu có
Yêu cầu kỹ thuật
không log password SMTP
không hardcode username/password
gửi email theo async/queue, không block request chính
---
4.7. Slack webhook sender
Tạo:
`SlackWebhookSenderService`
Task
gửi HTTP POST tới webhook URL
build payload rõ ràng theo incident
Nội dung message tối thiểu
severity
monitor name
endpoint
incident type
message
triggered time
Yêu cầu
timeout ngắn
retry nếu lỗi tạm thời
không log full webhook URL
response fail phải lưu vào `alert_deliveries.error_message`
---
4.8. Alert delivery service
Tạo:
`AlertDeliveryService`
Nhiệm vụ
tạo record `PENDING`
update sang `SENT` nếu gửi thành công
update sang `FAILED` nếu gửi lỗi
tăng `retry_count` khi retry
Flow chuẩn
tạo `alert_deliveries` status = `PENDING`
gọi sender tương ứng
nếu thành công:
status = `SENT`
set `sent_at`
nếu thất bại:
status = `FAILED`
set `error_message`
---
5. Redis caching
5.1. Mục tiêu cache
Dùng Redis để:
giảm tải aggregate query cho summary cards
giảm tải query list alerts page đầu
giảm tải query alert detail truy cập lặp lại
hỗ trợ invalidate nhanh khi incident thay đổi
5.2. Dữ liệu nên cache
A. Summary cards
API:
`GET /api/v1/alerts/summary`
Cache key:
`alerts:summary:{userId}:{range}`
TTL gợi ý:
30s đến 60s
B. Alerts list page đầu
API:
`GET /api/v1/alerts`
Chỉ cache các case phổ biến:
`page=0 hoặc 1`
`size=10 hoặc 20`
sort mặc định `triggered_at desc`
filter đơn giản
không cache search quá dài hoặc custom date phức tạp
Cache key gợi ý:
`alerts:list:{userId}:{hash(filters)}`
TTL gợi ý:
20s đến 60s
C. Alert detail
API:
`GET /api/v1/alerts/{id}`
Cache key:
`alerts:detail:{incidentId}`
TTL gợi ý:
30s đến 120s
5.3. Invalidate cache khi nào
Phải xóa cache khi:
tạo incident mới
update incident active
acknowledge incident
resolve incident
tạo delivery mới nếu API detail có hiển thị delivery history
cập nhật/xóa/toggle `alert_configs` nếu API detail hoặc test endpoint phụ thuộc dữ liệu config
5.4. Service cache
Tạo:
`AlertCacheService`
Nhiệm vụ
build key cache thống nhất
get/set cache
invalidate theo incident
invalidate summary theo user
invalidate list phổ biến theo user
5.5. Yêu cầu triển khai
không cache dữ liệu nhạy cảm như SMTP password
cache object response DTO, không cache entity JPA trực tiếp
với dữ liệu thay đổi thường xuyên, ưu tiên TTL ngắn + invalidate chủ động
fallback về DB bình thường nếu Redis lỗi
---
6. API phục vụ tab Alerts
6.1. GET `/api/v1/alerts/summary`
Mục tiêu:
lấy dữ liệu cho 4 summary cards
Query params
`range=24h|7d|30d`
Response mẫu
```json
{
  "totalAlerts": {
    "value": 1284,
    "changePercent": 12.0
  },
  "activeAlerts": {
    "value": 24,
    "urgentCount": 2
  },
  "criticalAlerts": {
    "value": 6,
    "actionRequired": true
  },
  "resolvedAlerts": {
    "value": 1254,
    "successRate": 98.0
  }
}
```
Logic
`totalAlerts`: số incident tạo trong range
`activeAlerts`: số incident `ACTIVE`
`urgentCount`: số incident `ACTIVE` và `CRITICAL`
`criticalAlerts`: số incident severity = `CRITICAL` và chưa resolved
`resolvedAlerts`: số incident `RESOLVED`
`successRate`: `resolved / total * 100`
Redis
cache response bằng key `alerts:summary:{userId}:{range}`
---
6.2. GET `/api/v1/alerts`
Mục tiêu:
lấy danh sách incident để render table
Query params
`page`
`size`
`search`
`status`
`severity`
`type`
`range`
`from`
`to`
`sort`
Cột cần trả ra
`id`
`time`
`apiName`
`endpoint`
`alertType`
`severity`
`status`
`message`
Response mẫu
```json
{
  "items": [
    {
      "id": "uuid",
      "time": "2026-04-02T12:04:12",
      "apiName": "Authentication Svc",
      "endpoint": "/v1/auth/login",
      "alertType": "API_DOWN",
      "severity": "CRITICAL",
      "status": "ACTIVE",
      "message": "Service unreachable from us-east-1 region"
    }
  ],
  "page": 1,
  "size": 10,
  "totalItems": 1284,
  "totalPages": 129
}
```
Search cần hỗ trợ
monitor name
endpoint/url
incident title
incident message
Sort mặc định
`triggered_at desc`
Redis
chỉ cache first-page query phổ biến
key: `alerts:list:{userId}:{hash(filters)}`
---
6.3. GET `/api/v1/alerts/{id}`
Mục tiêu:
xem chi tiết 1 incident
Response nên có
incident info
monitor info
delivery logs
timeline:
triggered
last seen
resolved
delivery history
Redis
cache response detail ngắn hạn theo `incidentId`
---
6.4. PATCH `/api/v1/alerts/{id}/acknowledge`
Mục tiêu:
xác nhận đã thấy alert
Logic
chỉ incident `ACTIVE` mới được acknowledge
update status sang `ACKNOWLEDGED`
invalidate summary, list, detail cache
---
6.5. PATCH `/api/v1/alerts/{id}/resolve`
Mục tiêu:
resolve thủ công nếu cần
Logic
update status sang `RESOLVED`
set `resolved_at`
invalidate summary, list, detail cache
---
6.6. GET `/api/v1/alerts/export`
Mục tiêu:
export incident list theo bộ lọc hiện tại
Query params
giống API list
Output
CSV
Cột export
Time
API Name
Endpoint
Alert Type
Severity
Status
Message
Triggered At
Resolved At
Redis
không cache file export
nếu cần có thể dùng Redis lưu trạng thái export job trong tương lai
---
7. API quản lý cấu hình alert
7.1. GET `/api/v1/monitors/{monitorId}/alert-configs`
Mục tiêu:
lấy toàn bộ config alert của monitor
7.2. POST `/api/v1/monitors/{monitorId}/alert-configs`
Mục tiêu:
tạo config mới cho Email hoặc Slack
Request mẫu - Email
```json
{
  "type": "EMAIL",
  "destination": "ops@company.com",
  "isEnabled": true
}
```
Request mẫu - Slack
```json
{
  "type": "SLACK",
  "destination": "https://hooks.slack.com/services/xxx",
  "isEnabled": true
}
```
Validate
EMAIL:
destination phải đúng format email
SLACK:
destination phải là webhook URL hợp lệ
Redis
sau create/update/delete/toggle config:
invalidate cache detail monitor nếu có
invalidate cache liên quan nếu list/detail có hiển thị config
7.3. PUT `/api/v1/monitors/{monitorId}/alert-configs/{id}`
Mục tiêu:
cập nhật config
7.4. PATCH `/api/v1/monitors/{monitorId}/alert-configs/{id}/toggle`
Mục tiêu:
bật/tắt config
7.5. DELETE `/api/v1/monitors/{monitorId}/alert-configs/{id}`
Mục tiêu:
xóa config
---
8. API test gửi notification
8.1. POST `/api/v1/alerts/test/email`
Mục tiêu:
test Brevo SMTP có hoạt động không
Request
```json
{
  "to": "your_email@example.com"
}
```
Logic
gửi 1 email test đơn giản
trả về success/fail
8.2. POST `/api/v1/alerts/test/slack`
Mục tiêu:
test Slack webhook
Request
```json
{
  "webhookUrl": "https://hooks.slack.com/services/xxx"
}
```
Logic
gửi 1 message test đơn giản
trả về success/fail
---
9. DTO cần tạo
Alert config
`CreateAlertConfigRequest`
`UpdateAlertConfigRequest`
`AlertConfigResponse`
Incident
`AlertListItemResponse`
`AlertListResponse`
`AlertSummaryResponse`
`AlertDetailResponse`
Test notification
`SendTestEmailRequest`
`SendTestSlackWebhookRequest`
---
10. Service cần tạo
`IncidentRuleEvaluator`
`IncidentService`
`AlertQueryService`
`AlertSummaryService`
`AlertExportService`
`AlertNotificationDispatcher`
`AlertDeliveryService`
`EmailSenderService`
`SlackWebhookSenderService`
`AlertConfigService`
`AlertCacheService`
---
11. Controller cần tạo
`AlertController`
`AlertConfigController`
`AlertTestController`
---
12. Luồng xử lý chuẩn
12.1. Incident phát sinh
scheduler/worker check monitor
evaluator phân tích kết quả
create hoặc update `incident`
invalidate Redis cache
dispatcher load `alert_configs`
tạo `alert_deliveries`
gửi email/slack
update trạng thái gửi
12.2. Recovery
monitor check thành công
tìm incident active
resolve incident
invalidate Redis cache
nếu có rule gửi recovery thì dispatch notification
lưu log gửi
---
13. Validation & security
Validation
email đúng format
slack webhook đúng format
không cho destination rỗng
không cho tạo config trùng vô nghĩa nếu muốn giới hạn
Security
chỉ truy cập monitor của chính user đó
không log secret
mask webhook khi trả ra response nếu cần
không trả SMTP password ra bất kỳ API nào
nếu Redis lỗi thì không làm fail toàn bộ request đọc
---
14. Logging & audit
Cần log:
incident được tạo
incident được update
incident được resolve
email gửi thành công/thất bại
slack gửi thành công/thất bại
retry notification
cache hit/miss cho summary và list phổ biến
Không log:
SMTP password
full webhook URL
credential nhạy cảm
---
15. Ưu tiên implement
Phase 1
entity + repository
create/list/update alert config
incident create/update/resolve
list alerts
summary alerts
Phase 2
Brevo SMTP email sender
Slack webhook sender
alert delivery log
Redis cache cho summary, list, detail
API test email/slack
Phase 3
export CSV
acknowledge/resolve manual
retry/backoff notification
tối ưu key invalidation
tối ưu query và warm cache nếu cần
---
16. Endpoint checklist
Alerts
[ ] GET `/api/v1/alerts/summary`
[ ] GET `/api/v1/alerts`
[ ] GET `/api/v1/alerts/{id}`
[ ] PATCH `/api/v1/alerts/{id}/acknowledge`
[ ] PATCH `/api/v1/alerts/{id}/resolve`
[ ] GET `/api/v1/alerts/export`
Alert configs
[ ] GET `/api/v1/monitors/{monitorId}/alert-configs`
[ ] POST `/api/v1/monitors/{monitorId}/alert-configs`
[ ] PUT `/api/v1/monitors/{monitorId}/alert-configs/{id}`
[ ] PATCH `/api/v1/monitors/{monitorId}/alert-configs/{id}/toggle`
[ ] DELETE `/api/v1/monitors/{monitorId}/alert-configs/{id}`
Test notification
[ ] POST `/api/v1/alerts/test/email`
[ ] POST `/api/v1/alerts/test/slack`
---
17. Ghi chú triển khai Brevo SMTP
dùng `spring-boot-starter-mail`
map env vào `spring.mail.*`
host dùng `smtp-relay.brevo.com`
port dùng `587`
auth = true
starttls = true
username/password lấy từ env

18. Ghi chú triển khai Redis
dùng `spring-boot-starter-data-redis`
serialize cache DTO bằng JSON
prefix key theo module `alerts:*`
tránh cache entity JPA có lazy relation
thêm timeout ngắn để dữ liệu không stale lâu
nên bọc cache bằng service riêng để dễ đổi chiến lược invalidate
---
19. Kết quả mong muốn
Sau khi hoàn thành các task trên, backend phải:
tạo incident từ monitor failure
hiển thị đúng tab Alerts
filter/search/paginate được
gửi được email qua Brevo SMTP
gửi được Slack Webhook
lưu được lịch sử gửi notification
export được alerts
cache được summary/list/detail bằng Redis để giảm tải DB