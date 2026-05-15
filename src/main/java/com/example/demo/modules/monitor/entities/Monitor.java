package com.example.demo.modules.monitor.entities;

import com.example.demo.modules.alert.entities.Incident;
import com.example.demo.modules.monitor.enums.MonitorStatus;
import com.example.demo.modules.uptimeLogs.entities.UptimeLogs;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "monitors", indexes = {
        @Index(name = "idx_monitor_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Monitor {

    /**
     * ID duy nhất của monitor.
     * Sử dụng UUID để tránh đoán được ID và thuận tiện khi scale hệ thống.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToMany(mappedBy = "monitor", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Incident> incidents = new ArrayList<>();

    @OneToMany(mappedBy = "monitor", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UptimeLogs> uptimeLogs = new ArrayList<>();

    /**
     * ID của user sở hữu monitor này.
     * Dùng để phân biệt dữ liệu monitor giữa các user.
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private com.example.demo.modules.user.entities.User user;

    /**
     * Tên monitor do người dùng đặt.
     * Ví dụ: "Google API", "Auth Service Production"
     */
    @Column(nullable = false)
    private String name;

    /**
     * URL endpoint cần được kiểm tra.
     * Ví dụ: https://api.example.com/health
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String url;

    /**
     * HTTP method dùng để gọi endpoint.
     * Mặc định là GET.
     * Có thể là GET, POST, PUT, PATCH, DELETE...
     */
    @Builder.Default
    @Column(length = 10)
    private String method = "GET";

    /**
     * Cấu hình xác thực request dưới dạng JSON.
     * Ví dụ:
     * { "type": "bearer", "token": "abc123" }
     * { "type": "basic", "username": "admin", "password": "123456" }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> auth;

    /**
     * Header của request dưới dạng key-value.
     * Ví dụ:
     * {
     * "Content-Type": "application/json",
     * "Authorization": "Bearer xxx"
     * }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> headers;

    /**
     * Query parameters gắn vào URL khi gửi request.
     * Ví dụ:
     * {
     * "page": "1",
     * "limit": "10"
     * }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> queryParams;

    /**
     * Nội dung body của request.
     * Chủ yếu dùng cho POST, PUT, PATCH.
     * Thường là JSON string.
     */
    @Column(columnDefinition = "TEXT")
    private String body;

    /**
     * Khoảng thời gian giữa hai lần kiểm tra liên tiếp, tính bằng giây.
     * Ví dụ: 60 nghĩa là kiểm tra mỗi 60 giây.
     */
    @Column(name = "check_interval")
    private Integer checkInterval;

    /**
     * Danh sách status code được xem là hợp lệ.
     * Lưu dạng chuỗi phân tách bằng dấu phẩy.
     * Ví dụ: "200,201,204"
     */
    @Builder.Default
    @Column(name = "expected_status_codes")
    private String expectedStatusCodes = "200,201";

    /**
     * Thời gian phản hồi tối đa cho phép (ms).
     * Nếu API phản hồi chậm hơn giá trị này thì có thể bị xem là warning/failed.
     */
    @Builder.Default
    @Column(name = "max_response_time_ms")
    private Integer maxResponseTimeMs = 1000;

    /**
     * Trạng thái bật/tắt monitor.
     * true: monitor đang hoạt động và sẽ được scheduler kiểm tra.
     * false: monitor bị tắt, không kiểm tra nữa.
     */
    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    /**
     * Trạng thái tắt cảnh báo.
     * true: có lỗi nhưng không gửi thông báo cảnh báo.
     * false: gửi cảnh báo bình thường.
     */
    @Builder.Default
    @Column(name = "is_muted")
    private Boolean isMuted = false;

    // ==============================
    // Các trường trạng thái gần nhất
    // Dùng để hiển thị dashboard nhanh
    // ==============================

    /**
     * Trạng thái lần kiểm tra gần nhất.
     * Ví dụ: Healthy, Warning, Down
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "last_status")
    private MonitorStatus lastStatus;

    /**
     * Độ trễ gần nhất đo được (ms).
     * Có thể dùng như network latency hoặc processing latency tùy cách đo.
     */
    @Column(name = "last_latency_ms")
    private Integer lastLatencyMs;

    /**
     * Thông điệp lỗi của lần kiểm tra gần nhất nếu có.
     * Ví dụ: timeout, connection refused, SSL handshake failed...
     */
    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;

    /**
     * Thời điểm hệ thống thực hiện lần kiểm tra gần nhất.
     */
    @Column(name = "last_check_at")
    private LocalDateTime lastCheckAt;

    /**
     * Số lần thất bại liên tiếp.
     * Dùng để xác định mức độ nghiêm trọng và tránh alert giả do lỗi tạm thời.
     */
    @Column(name = "consecutive_failures")
    private Integer consecutiveFailures;

    /**
     * Thời điểm dự kiến cho lần kiểm tra tiếp theo.
     * Hữu ích khi hiển thị scheduler/dashboard.
     */
    @Column(name = "next_check_at")
    private LocalDateTime nextCheckAt;

    /**
     * Thời điểm tạo monitor.
     * Tự động sinh khi insert bản ghi.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    /**
     * monitor cho bị chặn do admin không
     * **/
    @Builder.Default
    @Column(name = "is_block")
    private Boolean isBlock = false;
}