package com.example.demo.modules.notification.services;

import com.example.demo.modules.notification.dto.UserNotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class NotificationSseServiceImpl implements NotificationSseService {

    // Thời gian timeout của kết nối SSE (30 phút = 1,800,000 miligiây)
    private static final long SSE_TIMEOUT = 1800000L;
    
    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    @Override
    public SseEmitter subscribe(UUID userId) {
        // Tạo mới SseEmitter với thời gian timeout
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // Đăng ký các callback xử lý vòng đời kết nối để dọn dẹp bộ nhớ sạch sẽ
        emitter.onCompletion(() -> {
            log.info("[SSE] Kết nối của userId={} hoàn thành", userId);
            emitters.remove(userId);
        });

        emitter.onTimeout(() -> {
            log.info("[SSE] Kết nối của userId={} quá hạn (timeout)", userId);
            emitters.remove(userId);
        });

        emitter.onError((e) -> {
            log.warn("[SSE] Lỗi kết nối của userId={}: {}", userId, e.getMessage());
            emitters.remove(userId);
        });

        emitters.put(userId, emitter);
        log.info("[SSE] Đã đăng ký kết nối thành công cho userId={}. Số lượng online: {}", userId, emitters.size());

        // Gửi ngay 1 sự kiện giữ chỗ "connect" để hoàn tất việc bắt tay (handshake) và tránh timeout ban đầu
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("Connected successfully!"));
        } catch (IOException e) {
            log.error("[SSE] Không thể gửi sự kiện bắt tay cho userId={}", userId);
            emitters.remove(userId);
        }

        return emitter;
    }

    @Override
    public void sendNotification(UUID userId, UserNotificationResponse notification) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                // Đẩy sự kiện realtime có tên là "notification"
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(notification));
                log.info("[SSE] Đã đẩy thông báo tới userId={}", userId);
            } catch (IOException e) {
                log.warn("[SSE] Lỗi khi đẩy tin tới userId={}, tự động đóng kết nối: {}", userId, e.getMessage());
                emitters.remove(userId);
                emitter.completeWithError(e);
            }
        } else {
            log.debug("[SSE] Người dùng userId={} đang offline, không gửi SSE", userId);
        }
    }

    @Override
    public void closeAll() {
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.error("[SSE] Lỗi ngắt kết nối cho userId={}", userId);
            }
        });
        emitters.clear();
    }
}
