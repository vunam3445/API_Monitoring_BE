Task BE cho API phục vụ hiển thị trang Monitoring
Mục tiêu
Xây các API backend để FE render được toàn bộ trang Monitoring dựa trên dữ liệu từ `monitor` và `uptime_logs`, đồng thời có dùng Redis caching để giảm tải query aggregate, tăng tốc response và hỗ trợ dữ liệu gần realtime.
Phạm vi
Bao gồm:
API summary cards
API charts
API key API health
API recent monitoring events
API logs
API search/filter
API monitor detail/trend
Redis caching cho các API đọc dữ liệu
Không bao gồm:
CRUD monitor
Alerts tab
Dashboard tab
---
✅ 1. Chuẩn hóa contract dữ liệu phục vụ Monitoring UI
Task 1.1: Chuẩn hóa status monitor
- [x] Thống nhất enum trả ra cho FE:
`HEALTHY`
`WARNING`
`DOWN`
`PAUSED`
Done when
Mọi API monitoring đều trả status theo cùng một chuẩn
FE không phải tự map lại status
Task 1.2: Chuẩn hóa event type cho log/event
- [x] Thống nhất enum:
`HEALTH_CHECK_PASSED`
`SLOW_RESPONSE`
`API_FAILURE`
`TIMEOUT`
`RECOVERED`
Done when
Bảng Recent Monitoring Events và All Logs dùng chung một bộ event type
Task 1.3: Chuẩn hóa response DTO cho trang Monitoring
- [x] Tạo DTO riêng cho:
monitoring summary
chart point
key health card
recent event row
monitor log row
monitor overview
monitor trend
Done when
API response rõ ràng, ổn định, FE dễ tích hợp
---
✅ 2. Xây API summary cho header và summary cards
Task 2.1: Tạo API summary monitoring
- [x] Endpoint
`GET /api/monitoring/summary`
Dữ liệu trả về
`totalMonitors`
`healthyCount`
`warningCount` nếu muốn dùng nội bộ
`pausedCount`
`downCount`
`upCount`
`uptimePercentOverall`
`changeFromYesterday` nếu UI cần
Việc cần làm
Query tổng monitor theo user
Đếm theo trạng thái hiện tại
Xác định paused từ trạng thái hoặc cờ active
Tính uptime tổng quan từ `uptime_logs` trong khoảng thời gian đã chọn
Redis caching
Cache key gợi ý: `monitoring:summary:{userId}:{range}`
TTL gợi ý: 30–60 giây
Invalidate khi:
có check result mới
có monitor pause/resume
có thay đổi trạng thái monitor
Done when
FE gọi 1 API là render được:
Total APIs
Healthy
Paused
Down
badge UP / DOWN
---
3. Xây API biểu đồ Response Time
Task 3.1: Tạo API chart response time
Endpoint
`GET /api/monitoring/charts/response-time?range=1h`
`GET /api/monitoring/charts/response-time?range=24h&monitorId={id}`
Dữ liệu trả về
Danh sách điểm:
`time`
`avgResponseTimeMs`
Có thể mở rộng:
`minResponseTimeMs`
`maxResponseTimeMs`
Việc cần làm
Aggregate từ `uptime_logs`
Group theo bucket thời gian: 5m, 15m, 1h tùy range
Hỗ trợ filter theo từng monitor hoặc toàn bộ user
Sort theo thời gian tăng dần
Redis caching
Cache key gợi ý: `monitoring:chart:response-time:{userId}:{monitorId}:{range}`
TTL gợi ý: 30–120 giây
Dùng cache-aside
Invalidate khi có log mới của monitor tương ứng
Done when
FE render được chart API Response Time
---
4. Xây API biểu đồ Error Rate
Task 4.1: Tạo API chart error rate
Endpoint
`GET /api/monitoring/charts/error-rate?range=1h`
`GET /api/monitoring/charts/error-rate?range=24h&monitorId={id}`
Dữ liệu trả về
`time`
`errorRatePercent`
`totalChecks`
`failedChecks`
Việc cần làm
Xác định log thất bại
Aggregate từ `uptime_logs`
Tính `errorRatePercent = failedChecks / totalChecks * 100`
Group theo bucket thời gian
Redis caching
Cache key gợi ý: `monitoring:chart:error-rate:{userId}:{monitorId}:{range}`
TTL gợi ý: 30–120 giây
Invalidate khi có log mới
Done when
FE render được chart Error Rate
---
✅ 5. Xây API danh sách key health cards
Task 5.1: Tạo API key health cards
- [x] Endpoint
`GET /api/monitoring/key-health`
Dữ liệu trả về
`monitorId`
`monitorName`
`endpoint`
`currentStatus`
`latencyMs`
`uptimePercent`
`miniTrendData`: mảng response time gần nhất để vẽ sparkline
Việc cần làm
Lấy danh sách monitor của user
Với mỗi monitor, lấy status và latency mới nhất (từ Monitor entity)
Tính uptime percentage (thường là 24h)
Lấy 10–20 log gần nhất để làm sparkline
Redis caching
Key: `monitoring:key-health:{userId}`
TTL: 30–60 giây
Invalidate: giống summary
Done when
FE hiển thị được danh sách card ở tab Dashboard/Overview
---
✅ 6. Xây API Recent Monitoring Events
Task 6.1: Tạo API recent events
- [x] Endpoint
`GET /api/monitoring/events`
Dữ liệu trả về
`time`
`apiName`
`eventType`
`responseTime`
`status`
`message`
Việc cần làm
Lấy 10–20 sự kiện mới nhất trên toàn hệ thống (của user)
Map status theo logic Enum
Redis caching
Invalidate khi có log mới
Done when
Bảng Recent Monitoring Events ở Dashboard render đúng
---
✅ 7. Xây API All Monitoring Logs cho màn View All Logs
Task 7.1: Tạo API logs
- [x] Endpoint
`GET /api/monitoring/logs`
Dữ liệu trả về
`logId`
`monitorId`
`monitorName`
`checkedAt`
`responseTimeMs`
`statusCode`
`status`
`eventType`
`message`
`errorMessage`
Việc cần làm
Query từ bảng `uptime_logs`
Hỗ trợ filter monitorId, status, eventType
Done when
FE render được data table All Logs
---
✅ ✅ 8. Xây API search cho Monitoring
Task 8.1: Tạo API search
- [x] Endpoint
`GET /api/monitoring/search?keyword={keyword}`
Kết quả tìm kiếm
Có thể trả 2 nhóm:
`monitors`
`recentLogs`
Search theo
monitor name
endpoint/url
current status
event type
message
error message
Việc cần làm
Search monitor và log theo keyword
Giới hạn số lượng kết quả trả ra
Chuẩn hóa format cho FE search box
Redis caching
Cache key gợi ý: `monitoring:search:{userId}:{keyword}:{filtersHash}`
TTL gợi ý: 30 giây cho keyword phổ biến
Không cache keyword quá ngắn hoặc quá đặc thù nếu không cần
Done when
FE tìm được endpoint, status hoặc logs từ search box
---
✅ 9. Xây API filter monitor/list phục vụ hiển thị theo trạng thái
Task 9.1: Tạo API danh sách monitor phục vụ filter
- [x] Endpoint
`GET /api/Apis/user/{userId}?lastStatus=DOWN`
Dữ liệu trả về
`id`
`name`
`url`
`method`
`lastStatus`
`lastCheckedAt`
`lastResponseTimeMs`
Việc cần làm
Dùng `getApisByUser` hiện có
Support filter theo lastStatus
Done when
FE lọc được monitor theo status
---
✅ 10. Xây API pause/resume ảnh hưởng trực tiếp tới trang Monitoring
Task 10.1: Đảm bảo API pause/resume cập nhật được dữ liệu hiển thị
- [x] Endpoint hiện có
`PUT /api/Apis/{id}/status`
Việc cần làm
Sau pause/resume:
cập nhật trạng thái monitor
scheduler bỏ qua monitor paused
xóa/invalidate cache liên quan
Done when
Pause/resume xong, FE refresh là thấy số liệu mới đúng
---
✅ 11. Xây API monitor detail / overview
Task 11.1: Tạo API monitor overview
- [x] Endpoint
`GET /api/monitoring/{id}/overview`
Dữ liệu trả về
thông tin monitor cơ bản
current status
last check time
latest latency
uptime %
recent logs
chart data rút gọn nếu muốn
Việc cần làm
Tổng hợp monitor + thống kê + logs gần nhất
Kiểm tra quyền sở hữu monitor theo user
Redis caching
Invalidate khi có log mới của monitor này
Done when
FE có thể mở popup/detail page cho 1 monitor
---
✅ 12. Xây API monitor trend cho sparkline/mini chart
Task 12.1: Tạo API trend theo monitor
- [x] Endpoint
`GET /api/monitoring/{id}/trend`
Dữ liệu trả về
`monitorId`
`range`
`points`: danh sách giá trị trend
Việc cần làm
Lấy N log gần nhất
Convert về mảng đơn giản cho FE
Redis caching
Invalidate khi có log mới
Done when
FE vẽ được sparkline cho từng monitor
---
13. Redis caching layer cho toàn bộ Monitoring read APIs
Task 13.1: Tạo Redis cache service dùng chung
Việc cần làm
Viết cache abstraction/service chung:
`get`
`set`
`evict`
`evictByPattern` hoặc cơ chế version key
Chuẩn hóa prefix key cho monitoring
Done when
Các API đọc có thể tái sử dụng chung cache service
Task 13.2: Thiết kế cache key strategy
Việc cần làm
Quy ước key rõ ràng:
`monitoring:summary:*`
`monitoring:chart:*`
`monitoring:key-health:*`
`monitoring:events:*`
`monitoring:logs:*`
`monitoring:monitor:*`
Done when
Dễ debug cache, dễ invalidate
Task 13.3: Thiết kế cơ chế invalidation
Nguồn invalidate
có `uptime_logs` mới
monitor đổi status
monitor pause/resume
monitor bị xóa hoặc sửa config ảnh hưởng dữ liệu đọc
Cách làm gợi ý
Cách 1: xóa theo pattern
Cách 2: dùng version key theo user hoặc monitor để tránh quét nhiều key
Cách 3: publish event khi check result xong rồi worker xử lý invalidate
Done when
Cache không bị stale quá lâu và dữ liệu UI vẫn đủ mới
Task 13.4: Áp dụng cache-aside cho API đọc
Việc cần làm
Với mỗi API đọc:
sinh cache key
đọc Redis
nếu miss thì query DB
map DTO
set lại cache
Done when
API đọc dùng Redis ổn định, fallback DB bình thường khi cache miss
---
14. Tối ưu query cho các API aggregate
Task 14.1: Tối ưu query summary
Việc cần làm
Không load toàn bộ logs lên Java để tính
Dùng SQL aggregate/count trực tiếp
Task 14.2: Tối ưu query chart
Việc cần làm
Group by time bucket ở DB
Chỉ lấy range cần thiết
Không query dư dữ liệu ngoài khoảng thời gian FE yêu cầu
Task 14.3: Tối ưu query event/log list
Việc cần làm
Dùng pagination
Sort đúng index
Tránh N+1 khi join monitor
Done when
DB query gọn, kết hợp với Redis cho tốc độ tốt
---
15. Thêm index DB hỗ trợ Monitoring APIs
Task 15.1: Index cho bảng `monitor`
Nên có
`(user_id)`
`(user_id, status)`
`(user_id, is_active)` nếu có
Task 15.2: Index cho bảng `uptime_logs`
Nên có
`(monitor_id, checked_at desc)`
`(checked_at desc)`
`(monitor_id, status, checked_at desc)`
`(monitor_id, event_type, checked_at desc)` nếu có event_type
`(status, checked_at desc)` nếu hay filter status toàn user
Done when
API chart, event, logs query nhanh hơn rõ rệt
---
16. Bảo mật và phân quyền cho Monitoring APIs
Task 16.1: Filter dữ liệu theo user
Việc cần làm
Mọi API chỉ đọc monitor/log của user hiện tại
Task 16.2: Check ownership khi xem monitor detail/trend
Việc cần làm
Không cho truy cập monitor không thuộc user
Done when
Không rò dữ liệu giữa các user
---
17. Test cho Monitoring APIs và Redis caching
Task 17.1: Test API summary
dữ liệu monitor/status đúng
cache hit/miss đúng
Task 17.2: Test API charts
aggregate đúng
range đúng
cache đúng key
Task 17.3: Test API key-health
uptime %
latest latency
trend data
Task 17.4: Test recent events/logs
mapping event type đúng
pagination/filter đúng
Task 17.5: Test cache invalidation
thêm log mới -> summary/chart/event cache bị invalid
pause/resume -> summary/list cache bị invalid
Done when
Có test đủ cho luồng đọc dữ liệu monitoring và cache
---
18. Thứ tự ưu tiên triển khai
Priority 1
API `GET /api/monitoring/summary`
API `GET /api/monitoring/charts/response-time`
API `GET /api/monitoring/charts/error-rate`
API `GET /api/monitoring/key-health`
API `GET /api/monitoring/events/recent`
Redis cache service + cache key strategy
Priority 2
API `GET /api/monitoring/logs`
API `GET /api/monitoring/search`
API `GET /api/monitors?status=...`
cache invalidation theo log mới và pause/resume
Priority 3
API `GET /api/monitors/{id}/overview`
API `GET /api/monitors/{id}/trend`
tối ưu index/query sâu hơn
test cache/invalidation đầy đủ
---
19. Định nghĩa hoàn thành toàn bộ phần BE cho Monitoring page
Hoàn thành khi FE có thể dùng API để hiển thị đầy đủ:
summary cards
up/down badge
response time chart
error rate chart
key API health cards
recent monitoring events
all logs
search/filter
monitor detail/trend
và:
các API đọc quan trọng đã dùng Redis cache
có cơ chế invalidate hợp lý
dữ liệu không stale quá lâu
query DB đã được tối ưu đủ dùng