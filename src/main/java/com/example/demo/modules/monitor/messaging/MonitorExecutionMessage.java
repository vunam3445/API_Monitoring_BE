package com.example.demo.modules.monitor.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO message được gửi qua RabbitMQ.
 * Chứa thông tin tối thiểu cần thiết để Worker thực thi kiểm tra.
 *
 * Lý do chỉ gửi monitorId thay vì toàn bộ entity:
 * - Giảm kích thước message trên queue.
 * - Worker luôn lấy dữ liệu mới nhất từ DB, tránh stale data.
 * - Dễ retry vì chỉ cần ID.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitorExecutionMessage implements Serializable {

    /**
     * ID của monitor cần thực thi kiểm tra.
     */
    private String monitorId;

    /**
     * Thời điểm message được tạo (dùng để tracking/debug).
     */
    private LocalDateTime scheduledAt;
}
