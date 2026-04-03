package com.example.demo.modules.uptimeLogs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO cho UptimeLogs.
 * Dùng để trả kết quả kiểm tra API cho frontend hiển thị lịch sử.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UptimeLogResponse {

    private Long id;

    private UUID monitorId;

    /** Thông tin từ Monitor (Join) */
    private String monitorName;
    private String monitorUrl;
    private String monitorMethod;

    /** Thời điểm kiểm tra */
    private LocalDateTime checkedAt;

    /** HTTP status code (200, 404, 500...) */
    private Integer statusCode;

    /** Thời gian phản hồi (ms) */
    private Integer responseTimeMs;

    /** Endpoint hoạt động hay không */
    private Boolean isUp;

    /** Loại lỗi (TIMEOUT, DNS_ERROR, CONNECTION_REFUSED, SSL_ERROR...) */
    private String errorType;

    /** Chi tiết lỗi */
    private String errorMessage;

    /** Đoạn trích response body */
    private String responseSnippet;

    /** Kết quả assertion: PASSED / FAILED */
    private String assertionStatus;

    /** Chi tiết lý do assertion pass/fail */
    private String assertionMessage;
}

