package com.example.demo.modules.uptimeLogs.entities;

import com.example.demo.modules.monitor.entities.Monitor;
import com.example.demo.modules.monitor.enums.MonitorEventType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "uptime_logs",
        indexes = {
                @Index(name = "idx_uptime_logs_monitor_checked_at", columnList = "monitor_id, checked_at")
        }
)
@Data
public class UptimeLogs {

    /**
     * ID duy nhất của log.
     * Tăng tự động theo kiểu bigserial trong database.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID của monitor mà log này thuộc về.
     * Dùng để liên kết log với một monitor cụ thể.
     */
    @Column(name = "monitor_id", nullable = false, insertable = false, updatable = false)
    private UUID monitorId;

    /**
     * Mối quan hệ với monitor entity.
     * Cho phép truy xuất thông tin của API trực tiếp từ log (ví dụ: lấy tên, method).
     * Hỗ trợ join trong specification khi filter.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitor_id", referencedColumnName = "id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Monitor monitor;

    /**
     * Thời điểm hệ thống thực hiện kiểm tra uptime.
     * Tự động gán khi bản ghi được tạo.
     */
    @CreationTimestamp
    @Column(name = "checked_at", nullable = false, updatable = false)
    private LocalDateTime checkedAt;

    /**
     * HTTP status code nhận được từ endpoint.
     * Ví dụ: 200, 404, 500...
     * Có thể null nếu request không đi được tới server.
     */
    @Column(name = "status_code")
    private Integer statusCode;

    /**
     * Thời gian phản hồi của request, tính bằng mili giây.
     * Dùng để đo hiệu năng của endpoint.
     */
    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    /**
     * Kết quả tổng quát của lần check.
     * true: endpoint được xem là đang hoạt động.
     * false: endpoint bị down hoặc không đạt điều kiện kiểm tra.
     */
    @Column(name = "is_up", nullable = false)
    private Boolean isUp;

    /**
     * Loại lỗi xảy ra nếu request thất bại.
     * Ví dụ: TIMEOUT, DNS_ERROR, CONNECTION_REFUSED, SSL_ERROR...
     */
    @Column(name = "error_type")
    private String errorType;

    /**
     * Nội dung chi tiết lỗi của lần kiểm tra.
     * Ví dụ: "Connection timed out after 5000ms"
     */
    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    /**
     * Một phần nội dung response trả về từ endpoint.
     * Dùng để debug nhanh khi assertion thất bại hoặc response bất thường.
     * Chỉ nên lưu một đoạn ngắn, không nên lưu toàn bộ response lớn.
     */
    @Column(name = "response_snippet", columnDefinition = "text")
    private String responseSnippet;

    /**
     * Loại sự kiện (ví dụ: UP, DOWN, DEGRADED).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    private MonitorEventType eventType;

    /**
     * Kết quả kiểm tra assertion.
     * Ví dụ:
     * - PASSED: đạt điều kiện mong đợi
     * - FAILED: không đạt điều kiện mong đợi
     *
     * Assertion có thể dựa trên status code, response time, nội dung response...
     */
    @Column(name = "assertion_status", nullable = false)
    private String assertionStatus = "PASSED";

    /**
     * Thông điệp mô tả lý do assertion pass/fail.
     * Ví dụ:
     * - "Response time exceeded 1000ms"
     * - "Status code 500 is not in expected status codes"
     */
    @Column(name = "assertion_message", columnDefinition = "text")
    private String assertionMessage;
}
