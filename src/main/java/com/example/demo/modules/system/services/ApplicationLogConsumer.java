package com.example.demo.modules.system.services;

import com.example.demo.modules.system.entities.ApplicationLog;
import com.example.demo.modules.system.repositories.ApplicationLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * RabbitMQ Consumer: Tiêu thụ message log từ queue "system.logs.queue",
 * giải mã JSON và lưu vào bảng application_logs trong Database.
 *
 * Message được đẩy vào queue bởi RabbitMqLogAppender (Logback Custom Appender).
 */
@Service
@RequiredArgsConstructor
public class ApplicationLogConsumer {

    private final ApplicationLogRepository applicationLogRepository;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "system.logs.queue")
    public void consumeApplicationLog(byte[] messageBytes) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> logMap = objectMapper.readValue(messageBytes, Map.class);

            // Chuyển đổi timestamp từ epoch milliseconds sang LocalDateTime
            long timestampMs = ((Number) logMap.get("timestamp")).longValue();
            LocalDateTime timestamp = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(timestampMs), ZoneId.systemDefault());

            ApplicationLog appLog = ApplicationLog.builder()
                    .timestamp(timestamp)
                    .level((String) logMap.get("level"))
                    .component((String) logMap.get("component"))
                    .threadId((String) logMap.get("threadId"))
                    .message((String) logMap.get("message"))
                    .stackTrace((String) logMap.get("stackTrace"))
                    .build();

            applicationLogRepository.save(appLog);
        } catch (Exception e) {
            // Dùng System.err thay vì logger để tránh tạo vòng lặp log đệ quy vô hạn
            System.err.println("[ApplicationLogConsumer] Failed to persist application log: " + e.getMessage());
        }
    }
}
