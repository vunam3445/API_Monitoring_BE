package com.example.demo.modules.monitor.execution;

import com.example.demo.modules.monitor.entities.Monitor;
import com.example.demo.modules.uptimeLogs.entities.UptimeLogs;

/**
 * Interface trừu tượng hóa việc thực thi gọi API.
 *
 * Áp dụng Dependency Inversion:
 * - Worker phụ thuộc vào interface này, không phụ thuộc trực tiếp vào WebClient.
 * - Có thể thay bằng HttpClient, OkHttp... mà không sửa Worker.
 * - Dễ mock trong unit test.
 */
public interface ApiExecutionService {

    /**
     * Thực thi gọi API endpoint của monitor và trả về kết quả dưới dạng UptimeLogs.
     *
     * @param monitor Entity chứa đầy đủ cấu hình (URL, method, headers, body, auth, assertions...)
     * @param timeoutMs Thời gian chờ tối đa cho request (ms)
     * @return UptimeLogs đã được populate đầy đủ thông tin kết quả (chưa persist)
     */
    UptimeLogs execute(Monitor monitor, Integer timeoutMs);
}
