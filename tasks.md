TASK: Backend APIs for Dashboard Page
1. Mục tiêu
Hoàn thiện các API backend để render đầy đủ giao diện Dashboard / System Overview theo mockup đã cung cấp, có Redis caching, và có cơ chế kiểm tra API đã tồn tại thì bỏ qua, chỉ code phần còn thiếu.
---
2. Nguyên tắc thực hiện
2.1. Rule kiểm tra trước khi code
Trước khi implement bất kỳ API/service/repository/query nào, cần kiểm tra:
Đã có controller endpoint đó chưa.
Đã có service method xử lý nghiệp vụ đó chưa.
Đã có repository query hoặc specification phục vụ dữ liệu đó chưa.
Đã có DTO/response model phù hợp chưa.
2.2. Cách xử lý
Nếu đã có API đáp ứng đúng nghiệp vụ và response gần đúng FE cần:
Không code lại.
Chỉ refactor nhỏ hoặc mở rộng response nếu thực sự cần.
Nếu đã có một phần:
Tái sử dụng phần cũ.
Chỉ bổ sung field thiếu, query thiếu, cache thiếu.
Nếu chưa có:
Mới tạo controller/service/repository/DTO đầy đủ.
2.3. Yêu cầu bắt buộc khi review code trước khi làm
Cần rà soát các thành phần hiện có liên quan đến:
`monitors`
`uptime_logs`
`incidents` / `alerts` / `alert_deliveries` nếu đã có
dashboard/statistics/report APIs đã tồn tại
redis cache service hiện tại trong project
pagination/filter DTO đã có sẵn
base response / generic response đang dùng trong project
---
3. Phạm vi giao diện cần hỗ trợ
Dashboard hiện tại cần dữ liệu cho các block sau:
3.1. Summary cards
Total Monitors
Currently Down
24h Uptime
Avg Latency
Có thêm delta/trend so với kỳ trước để hiển thị `+4`, `-2`, `+0.02%`, `+12ms`
3.2. Charts / widgets
Avg. Response Time - Last 24H (bar chart)
Error Rate (%) - Global theo từng service/monitor nhóm
API Uptime (gauge / donut)
3.3. Unstable monitors section
Top 5 Unstable Monitors
Mỗi monitor hiển thị:
rank
monitor id
monitor name
downtime duration
incident count
health score / stability score
status color
3.4. Plan usage widget
Current plan
Monitor usage: số monitor đã dùng / quota tối đa
remaining slots
CTA upgrade chỉ cần BE trả dữ liệu plan/quota, FE tự render button
3.5. System suggestion
Gợi ý dạng text như:
monitor nào có error rate cao nhất
nên review check interval / timeout / expected status
Có thể trả về rule-based suggestion, chưa cần AI
---
4. Đề xuất API cần có
4.1. Dashboard summary API
Endpoint
`GET /api/v1/dashboard/summary`
Mục đích
Trả dữ liệu cho 4 card đầu trang.
Response mẫu
```json
{
  "totalMonitors": 128,
  "currentlyDown": 4,
  "uptime24h": 99.98,
  "avgLatencyMs": 245,
  "trends": {
    "totalMonitorsDelta": 4,
    "currentlyDownDelta": -2,
    "uptime24hDelta": 0.02,
    "avgLatencyDeltaMs": 12
  },
  "comparisonWindow": "previous_24h"
}
```
Nghiệp vụ
`totalMonitors`: tổng số monitor đang active của user.
`currentlyDown`: số monitor có trạng thái down tại thời điểm hiện tại.
`uptime24h`: uptime trung bình 24h gần nhất.
`avgLatencyMs`: latency trung bình 24h gần nhất.
Trend được tính bằng cách so sánh với 24h liền trước đó.
---
4.2. Dashboard response time chart API
Endpoint
`GET /api/v1/dashboard/charts/response-time?range=24h&bucket=hour`
Mục đích
Render chart Avg. Response Time.
Response mẫu
```json
{
  "range": "24h",
  "bucket": "hour",
  "points": [
    { "time": "2026-04-02T00:00:00", "avgLatencyMs": 210 },
    { "time": "2026-04-02T01:00:00", "avgLatencyMs": 225 },
    { "time": "2026-04-02T02:00:00", "avgLatencyMs": 240 }
  ]
}
```
Nghiệp vụ
Dữ liệu được bucket theo giờ.
Mỗi bucket tính trung bình latency từ `uptime_logs`.
Chỉ lấy monitor thuộc user hiện tại.
Nếu không có data ở bucket nào thì trả `0` hoặc `null` theo convention thống nhất với FE.
---
4.3. Dashboard error rate API
Endpoint
`GET /api/v1/dashboard/error-rate?range=24h&limit=5`
Mục đích
Render block Error Rate (%).
Response mẫu
```json
{
  "range": "24h",
  "items": [
    {
      "monitorId": "uuid-1",
      "monitorName": "Auth Service",
      "errorRate": 0.12,
      "totalChecks": 1200,
      "failedChecks": 1,
      "severity": "LOW"
    },
    {
      "monitorId": "uuid-2",
      "monitorName": "Payments",
      "errorRate": 4.85,
      "totalChecks": 900,
      "failedChecks": 44,
      "severity": "MEDIUM"
    }
  ]
}
```
Nghiệp vụ
`errorRate = failedChecks / totalChecks * 100`
Có rule phân loại severity:
`LOW`: < 1%
`MEDIUM`: 1% - < 5%
`HIGH`: >= 5%
Sắp xếp giảm dần theo error rate.
---
4.4. Dashboard uptime gauge API
Endpoint
`GET /api/v1/dashboard/uptime-gauge?range=24h`
Mục đích
Render widget donut/gauge API uptime.
Response mẫu
```json
{
  "range": "24h",
  "uptimePercentage": 99.2,
  "successfulChecks": 12400,
  "totalChecks": 12500
}
```
Nghiệp vụ
Tính uptime tổng hợp trên toàn bộ monitor của user trong 24h.
Có thể tái sử dụng logic summary nếu phù hợp.
---
4.5. Top unstable monitors API
Endpoint
`GET /api/v1/dashboard/unstable-monitors?range=7d&limit=5`
Mục đích
Render block Top 5 Unstable Monitors.
Response mẫu
```json
{
  "range": "7d",
  "items": [
    {
      "rank": 1,
      "monitorId": "uuid-1",
      "monitorName": "Inventory API",
      "downtimeMinutes": 252,
      "incidentCount": 12,
      "errorRate": 12.4,
      "stabilityScore": 41.2,
      "currentStatus": "DOWN",
      "statusColor": "RED"
    }
  ]
}
```
Nghiệp vụ
Xếp hạng monitor không ổn định dựa trên score tổng hợp. Có thể dùng công thức rule-based như:
```text
stabilityScore = 100
                 - (downtimeWeight * downtimeMinutes)
                 - (incidentWeight * incidentCount)
                 - (errorRateWeight * errorRate)
```
Hoặc chuẩn hóa về thang điểm 0-100.
Gợi ý trọng số
downtime: 0.4
incident count: 0.3
error rate: 0.3
Kết quả
Sort tăng dần theo stability score hoặc giảm dần theo mức độ bất ổn.
Chỉ lấy top `limit`.
---
4.6. Dashboard plan usage API
Endpoint
`GET /api/v1/dashboard/plan-usage`
Mục đích
Render widget Current Plan.
Response mẫu
```json
{
  "planName": "Professional",
  "monitorLimit": 20,
  "usedMonitors": 15,
  "remainingMonitors": 5,
  "usagePercentage": 75,
  "canUpgrade": true
}
```
Nghiệp vụ
Lấy từ subscription hiện tại của user hoặc plan hiện tại trong user/profile/subscription.
Nếu chưa có subscription thì fallback theo free plan.
`usedMonitors`: số monitor đang active.
`monitorLimit`: quota từ subscription plan.
---
4.7. Dashboard suggestion API
Endpoint
`GET /api/v1/dashboard/suggestions`
Mục đích
Render block System Suggestion.
Response mẫu
```json
{
  "title": "SYSTEM SUGGESTION",
  "message": "Inventory API has the highest error rate. Consider reviewing check interval, timeout, and expected status.",
  "relatedMonitorId": "uuid-1",
  "relatedMonitorName": "Inventory API",
  "suggestionType": "HIGH_ERROR_RATE"
}
```
Nghiệp vụ
Rule-based:
Nếu có monitor có error rate cao nhất và vượt ngưỡng cảnh báo => sinh suggestion.
Nếu không, fallback sang monitor có downtime cao nhất.
Nếu toàn hệ thống ổn định => trả message mặc định tích cực.
---
4.8. Optional: Dashboard aggregate API
Nếu muốn giảm số lượng request từ FE, có thể thêm API tổng hợp:
Endpoint
`GET /api/v1/dashboard/overview`
Mục đích
Trả toàn bộ dữ liệu dashboard trong một lần gọi.
Gợi ý
API này có thể orchestration từ các service con:
summary
response time chart
error rate
uptime gauge
unstable monitors
plan usage
suggestion
Lưu ý:
Nếu project đang ưu tiên FE load nhanh bằng 1 request, nên làm.
Nếu project đang có sẵn các endpoint nhỏ, có thể giữ các API riêng và chỉ thêm overview nếu cần.
---
5. Redis caching requirements
5.1. Mục tiêu cache
Giảm số lần query aggregate nặng lên PostgreSQL, đặc biệt các API dashboard được gọi thường xuyên.
5.2. Các API cần cache
Ưu tiên cache cho:
`GET /dashboard/summary`
`GET /dashboard/charts/response-time`
`GET /dashboard/error-rate`
`GET /dashboard/uptime-gauge`
`GET /dashboard/unstable-monitors`
`GET /dashboard/plan-usage`
`GET /dashboard/suggestions`
`GET /dashboard/overview` nếu có
5.3. Cache key convention
Format gợi ý:
```text
dashboard:summary:{userId}
dashboard:response-time:{userId}:{range}:{bucket}
dashboard:error-rate:{userId}:{range}:{limit}
dashboard:uptime-gauge:{userId}:{range}
dashboard:unstable-monitors:{userId}:{range}:{limit}
dashboard:plan-usage:{userId}
dashboard:suggestions:{userId}
dashboard:overview:{userId}:{range}
```
5.4. TTL gợi ý
Summary: `60 - 120s`
Response time chart: `60 - 300s`
Error rate: `60 - 300s`
Uptime gauge: `60 - 120s`
Unstable monitors: `300s`
Plan usage: `300 - 900s`
Suggestions: `120 - 300s`
Overview: `60 - 180s`
5.5. Invalidate cache
Khi có thay đổi dữ liệu liên quan, cần xóa cache theo pattern hoặc theo key cụ thể:
monitor được tạo/sửa/xóa/enable/disable
có log check mới được ghi vào `uptime_logs`
incident mới phát sinh hoặc incident được resolve
subscription/user plan thay đổi
Yêu cầu
Viết helper/service để clear cache dashboard theo `userId`.
Không hardcode xóa từng key rời rạc trong controller.
Gom vào `DashboardCacheService` hoặc tương đương.
---
6. Yêu cầu dữ liệu và query
6.1. Nguồn dữ liệu chính
Ưu tiên dùng:
`monitors`
`uptime_logs`
`incidents` nếu đã có
`subscriptions` / `subscription_plans` / `users.plan_type` tùy cấu trúc hiện tại
6.2. Query aggregate cần tối ưu
Cần tối ưu các query dạng:
count monitor theo status
avg latency theo time bucket
uptime % theo khoảng thời gian
failed checks / total checks
tổng downtime theo monitor
số incident theo monitor
top unstable monitors
6.3. Yêu cầu tối ưu
Chỉ select field cần thiết.
Tránh load entity full nếu chỉ cần aggregate.
Ưu tiên projection / DTO query / native query khi cần thiết.
Có index phù hợp trên:
`uptime_logs(monitor_id, checked_at)` hoặc tương đương
`uptime_logs(status, checked_at)`
`incidents(monitor_id, started_at, resolved_at)` nếu có
`monitors(user_id, is_active)`
---
7. Business rules chi tiết
7.1. Currently Down
Một monitor được coi là currently down nếu:
trạng thái lần check gần nhất là `DOWN`, hoặc
đang có incident open chưa resolve.
Ưu tiên dùng rule nào đang thống nhất trong hệ thống. Không tạo định nghĩa mới nếu project đã có sẵn.
7.2. Uptime
```text
uptime = successful_checks / total_checks * 100
```
Trong đó:
successful checks: status `UP` / `HEALTHY`
failed checks: status `DOWN` / `TIMEOUT` / `ERROR` tùy enum hiện tại
7.3. Avg latency
Chỉ tính trên các log có response time hợp lệ.
Cần thống nhất có tính log failed hay không.
Gợi ý: chỉ tính log có `response_time_ms != null`.
7.4. Downtime duration
Nếu có bảng incident:
lấy tổng thời lượng incident trong khoảng thời gian.
Nếu chưa có bảng incident:
tạm suy ra từ uptime_logs theo số bucket/check failed liên tiếp.
nhưng ưu tiên incident nếu đã tồn tại vì chính xác hơn.
---
8. Cấu trúc code mong muốn
8.1. Controller
Tạo `DashboardController` nếu chưa có.
8.2. Service
Tạo hoặc bổ sung:
`DashboardService`
`DashboardQueryService` nếu muốn tách read-only aggregate logic
`DashboardCacheService`
8.3. DTOs
Tạo DTO riêng cho từng API, ví dụ:
`DashboardSummaryResponse`
`DashboardTrendResponse`
`ResponseTimeChartResponse`
`ResponseTimePointResponse`
`ErrorRateItemResponse`
`UptimeGaugeResponse`
`UnstableMonitorItemResponse`
`PlanUsageResponse`
`DashboardSuggestionResponse`
`DashboardOverviewResponse`
8.4. Repository
Chỉ tạo query mới nếu query cũ không đáp ứng được.
Có thể bổ sung:
`MonitorRepository` aggregate methods
`UptimeLogRepository` aggregate methods
`IncidentRepository` aggregate methods
`SubscriptionRepository` / `SubscriptionPlanRepository`
---
9. Definition of Done
Hoàn thành task khi đạt đủ:
9.1. Functional
Dashboard load được toàn bộ block theo mockup.
API trả đúng dữ liệu cho từng widget.
Có xử lý trường hợp không có dữ liệu.
Có logic skip nếu API đã tồn tại.
9.2. Technical
Có Redis cache cho các API dashboard.
Có cơ chế clear cache phù hợp.
Không duplicate logic nếu project đã có service tương tự.
Query aggregate đủ tối ưu.
Code tuân thủ structure hiện có của project.
9.3. Testing
Cần có test hoặc ít nhất kiểm tra thủ công cho các case:
user chưa có monitor
user có monitor nhưng chưa có logs
user có nhiều monitor active/inactive
có monitor đang down
có incident open
có subscription plan và không có subscription plan
cache hit / cache miss / cache invalidate
---
10. Checklist implement
[ ] Rà soát API/dashboard/statistics hiện có, đánh dấu phần nào đã tồn tại
[ ] Rà soát DTO/service/repository hiện có để tái sử dụng
[ ] Tạo `DashboardController` nếu chưa có
[ ] Code `GET /dashboard/summary` nếu chưa có
[ ] Code `GET /dashboard/charts/response-time` nếu chưa có
[ ] Code `GET /dashboard/error-rate` nếu chưa có
[ ] Code `GET /dashboard/uptime-gauge` nếu chưa có
[ ] Code `GET /dashboard/unstable-monitors` nếu chưa có
[ ] Code `GET /dashboard/plan-usage` nếu chưa có
[ ] Code `GET /dashboard/suggestions` nếu chưa có
[ ] Cân nhắc code `GET /dashboard/overview` nếu FE muốn 1 request tổng
[ ] Thêm Redis caching cho các API dashboard
[ ] Thêm cơ chế invalidate dashboard cache theo `userId`
[ ] Kiểm tra index/query performance
[ ] Test dữ liệu thực tế với FE dashboard
---
11. Ghi chú cho developer
Không code trùng API đã tồn tại.
Không tạo endpoint mới nếu endpoint cũ chỉ thiếu vài field có thể mở rộng được.
Ưu tiên tái sử dụng logic hiện có từ monitoring, uptime logs, incidents, subscription.
Nếu dữ liệu mockup khác với cấu trúc DB thực tế, ưu tiên bám vào dữ liệu thực tế của hệ thống.
Nếu cần, có thể gộp một số widget về chung `overview` để giảm số request từ FE.