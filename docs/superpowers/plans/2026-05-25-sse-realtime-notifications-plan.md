# SSE Real-Time Notifications Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Triển khai luồng đẩy thông báo thời gian thực (Push Notification) từ Spring Boot backend đến React frontend thông qua Server-Sent Events (SSE) sử dụng Polyfill để truyền token qua Header Authorization bảo mật.

**Architecture:** 
1. `NotificationSseService` chịu trách nhiệm đăng ký kết nối `SseEmitter` cho từng user khi đăng nhập và phát tán (broadcast) sự kiện `"notification"` đến đúng user đang kết nối.
2. Endpoint `/api/v1/notifications/subscribe` trong `UserNotificationController` cho phép frontend bắt tay kết nối SSE.
3. Khi `NotificationBroadcastConsumer` nhận tin nhắn từ RabbitMQ, nếu `sendWeb` bật, nó sẽ batch insert vào DB và đẩy tức thì qua `NotificationSseService`.
4. Frontend hook `useUserNotifications.js` mở kết nối bằng `EventSourcePolyfill`, lắng nghe sự kiện để cập nhật trực tiếp danh sách và badge mà không cần polling 30 giây.

**Tech Stack:** Spring Boot, SseEmitter, EventSourcePolyfill (React), RabbitMQ.

---

### Task 1: Backend - Xây dựng dịch vụ NotificationSseService

**Files:**
- Create: `src/main/java/com/example/demo/modules/notification/services/NotificationSseService.java`
- Create: `src/main/java/com/example/demo/modules/notification/services/NotificationSseServiceImpl.java`

- [ ] **Step 1: Tạo Interface NotificationSseService**

Viết mã nguồn định nghĩa Interface quản lý kết nối tại `src/main/java/com/example/demo/modules/notification/services/NotificationSseService.java`:
```java
package com.example.demo.modules.notification.services;

import com.example.demo.modules.notification.dto.UserNotificationResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.UUID;

public interface NotificationSseService {
    SseEmitter subscribe(UUID userId);
    void sendNotification(UUID userId, UserNotificationResponse notification);
    void closeAll();
}
```

- [ ] **Step 2: Tạo lớp triển khai NotificationSseServiceImpl**

Viết mã triển khai quản lý Registry thread-safe và duy trì kết nối tại `src/main/java/com/example/demo/modules/notification/services/NotificationSseServiceImpl.java`:
```java
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
```

- [ ] **Step 3: Xác minh biên dịch**

Chạy lệnh để đảm bảo code dịch vụ SSE được viết hoàn toàn đúng cú pháp Java:
`.\mvnw compile`
Mong đợi: BUILD SUCCESS

---

### Task 2: Backend - Cấu hình Endpoint và Tích hợp Consumer

**Files:**
- Modify: `src/main/java/com/example/demo/modules/notification/controllers/UserNotificationController.java`
- Modify: `src/main/java/com/example/demo/modules/notification/services/NotificationBroadcastConsumer.java`

- [ ] **Step 1: Bổ sung endpoint đăng ký SSE ở UserNotificationController**

Thay thế nội dung file `src/main/java/com/example/demo/modules/notification/controllers/UserNotificationController.java` để khai báo Route `/subscribe`:
```java
package com.example.demo.modules.notification.controllers;

import com.example.demo.common.security.ISecurityContextService;
import com.example.demo.modules.notification.dto.UserNotificationResponse;
import com.example.demo.modules.notification.services.NotificationService;
import com.example.demo.modules.notification.services.NotificationSseService;
import com.example.demo.modules.user.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class UserNotificationController {

    private final NotificationService notificationService;
    private final NotificationSseService notificationSseService;
    private final ISecurityContextService securityContextService;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> subscribe() {
        UUID userId = getCurrentUserId();
        SseEmitter emitter = notificationSseService.subscribe(userId);
        return ResponseEntity.ok()
                .header("Content-Type", "text/event-stream")
                .header("Cache-Control", "no-cache")
                .header("Connection", "keep-alive")
                .header("X-Accel-Buffering", "no")
                .body(emitter);
    }

    @GetMapping
    public ResponseEntity<Page<UserNotificationResponse>> getMyNotifications(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = getCurrentUserId();
        Page<UserNotificationResponse> result = notificationService.getUserNotifications(
                userId, unreadOnly,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        UUID userId = getCurrentUserId();
        long count = notificationService.countUnread(userId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id) {
        UUID userId = getCurrentUserId();
        notificationService.markAsRead(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        UUID userId = getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }

    private UUID getCurrentUserId() {
        return securityContextService.getCurrentUser()
                .map(User::getId)
                .orElseThrow(() -> new IllegalStateException("Người dùng chưa đăng nhập"));
    }
}
```

- [ ] **Step 2: Tích hợp phát SSE trong RabbitMQ Consumer**

Chỉnh sửa file `src/main/java/com/example/demo/modules/notification/services/NotificationBroadcastConsumer.java` để đẩy tin nhắn sang `NotificationSseService` khi tiêu thụ hàng đợi thành công:
```java
package com.example.demo.modules.notification.services;

import com.example.demo.modules.alert.services.EmailSenderService;
import com.example.demo.modules.notification.config.NotificationMQConfig;
import com.example.demo.modules.notification.dto.NotificationBroadcastEvent;
import com.example.demo.modules.notification.dto.UserNotificationResponse;
import com.example.demo.modules.notification.entities.Notification;
import com.example.demo.modules.notification.entities.UserNotification;
import com.example.demo.modules.notification.repositories.NotificationRepository;
import com.example.demo.modules.notification.repositories.UserNotificationRepository;
import com.example.demo.modules.user.entities.User;
import com.example.demo.modules.user.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationBroadcastConsumer {

    private final NotificationRepository notificationRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final UserRepository userRepository;
    private final EmailSenderService emailSenderService;
    private final NotificationSseService notificationSseService;

    @RabbitListener(queues = NotificationMQConfig.BROADCAST_QUEUE)
    @Transactional
    public void consume(NotificationBroadcastEvent event) {
        log.info("[NotificationConsumer] Nhận sự kiện phát thông báo id={}, target={}/{}",
                event.notificationId(), event.targetType(), event.targetValue());

        try {
            Notification notification = notificationRepository.findById(event.notificationId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Notification not found: " + event.notificationId()));

            List<User> recipients = resolveRecipients(event);
            log.info("[NotificationConsumer] Số người nhận: {}", recipients.size());

            if (event.sendWeb()) {
                List<UserNotification> records = recipients.stream()
                        .map(user -> UserNotification.builder()
                                .user(user)
                                .notification(notification)
                                .build())
                        .collect(Collectors.toList());
                List<UserNotification> savedRecords = userNotificationRepository.saveAll(records);
                log.info("[NotificationConsumer] Đã tạo {} bản ghi web notification", savedRecords.size());

                // Gửi đẩy Server-Sent Events thời gian thực cho từng User nhận đang Online
                for (UserNotification record : savedRecords) {
                    try {
                        UserNotificationResponse responseDto = UserNotificationResponse.from(record);
                        notificationSseService.sendNotification(record.getUser().getId(), responseDto);
                    } catch (Exception sseEx) {
                        log.warn("[NotificationConsumer] Lỗi gửi SSE tới userId={}: {}", 
                                record.getUser().getId(), sseEx.getMessage());
                    }
                }
            }

            if (event.sendEmail()) {
                for (User user : recipients) {
                    try {
                        emailSenderService.sendNotificationEmail(
                                user.getEmail(),
                                event.title(),
                                event.content(),
                                event.level()
                        );
                    } catch (Exception e) {
                        log.error("[NotificationConsumer] Lỗi gửi email tới {}: {}", user.getEmail(), e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            log.error("[NotificationConsumer] Lỗi xử lý sự kiện id={}: {}", event.notificationId(), e.getMessage());
            throw new RuntimeException("Failed to process notification broadcast event", e);
        }
    }

    private List<User> resolveRecipients(NotificationBroadcastEvent event) {
        return switch (event.targetType()) {
            case "ALL" -> userRepository.findAll().stream()
                    .filter(u -> !"ADMIN".equals(u.getRole().name()))
                    .collect(Collectors.toList());

            case "PLAN" -> userRepository.findAll().stream()
                    .filter(u -> event.targetValue() != null
                            && event.targetValue().equalsIgnoreCase(u.getPlanType()))
                    .collect(Collectors.toList());

            case "SINGLE" -> userRepository.findByEmail(event.targetValue())
                    .map(List::of)
                    .orElse(List.of());

            default -> {
                log.warn("[NotificationConsumer] Loại target không hợp lệ: {}", event.targetType());
                yield List.of();
            }
        };
    }
}
```

- [ ] **Step 3: Xác minh biên dịch hệ thống**

Chạy lệnh biên dịch toàn diện backend để đảm bảo không lỗi:
`.\mvnw compile`
Mong đợi: BUILD SUCCESS

---

### Task 3: Frontend - Chuyển đổi từ Polling sang lắng nghe thời gian thực SSE

**Files:**
- Modify: `src/components/Layout/hooks/useUserNotifications.js`

- [ ] **Step 1: Cập nhật luồng kết nối SSE bảo mật qua Polyfill**

Thay thế hoàn chỉnh nội dung file `e:\API Monitoring FE\src\components\Layout\hooks\useUserNotifications.js` để tích hợp EventSourcePolyfill lắng nghe đẩy sự kiện trực tiếp:
```javascript
import { useState, useEffect, useCallback } from 'react';
import { userNotificationService } from '../../../services/userNotificationService';
import { EventSourcePolyfill } from 'event-source-polyfill';

export const useUserNotifications = () => {
    const [notifications, setNotifications] = useState([]);
    const [loading, setLoading] = useState(false);
    const [selectedNotification, setSelectedNotification] = useState(null);

    // Tính toán số thông báo chưa đọc
    const unreadCount = notifications.filter(n => !(n.isRead || n.read)).length;

    // Tải danh sách thông báo qua REST API lúc ban đầu
    const fetchNotifications = useCallback(async () => {
        setLoading(true);
        try {
            const data = await userNotificationService.getNotifications();
            // Đảm bảo map chuẩn các trường isRead từ server
            const formatted = data.map(item => ({
                ...item,
                read: item.isRead || item.read || false
            }));
            setNotifications(formatted);
        } catch (error) {
            console.error('Failed to fetch user notifications:', error);
        } finally {
            setLoading(false);
        }
    }, []);

    // 1. Khởi chạy fetch lần đầu khi mount
    useEffect(() => {
        fetchNotifications();
    }, [fetchNotifications]);

    // 2. Thiết lập kết nối SSE thời gian thực qua Polyfill
    useEffect(() => {
        const token = localStorage.getItem('accessToken');
        if (!token) return;

        const API_DOMAIN = import.meta.env.VITE_API_DOMAIN || 'http://localhost:8080/';
        const url = `${API_DOMAIN}api/v1/notifications/subscribe`;

        logSseDebug("Đang kết nối SSE an toàn với Header Authorization...");
        
        const eventSource = new EventSourcePolyfill(url, {
          headers: {
            'Authorization': `Bearer ${token}`
          },
          heartbeatTimeout: 1800000 // 30 phút theo timeout của Spring Boot emitter
        });

        // Lắng nghe sự kiện bắt tay kết nối thành công
        eventSource.addEventListener("connect", (event) => {
            console.log("[SSE] Bắt tay thành công:", event.data);
        });

        // Lắng nghe sự kiện "notification" đẩy trực tiếp từ server
        eventSource.addEventListener("notification", (event) => {
            try {
                const newNotif = JSON.parse(event.data);
                console.log("[SSE] Nhận thông báo realtime:", newNotif);

                // Tự động chuẩn hóa trường đọc
                const formattedNotif = {
                    ...newNotif,
                    read: newNotif.isRead || newNotif.read || false
                };

                // Đẩy thông báo mới lên đầu danh sách state tức thì mà không cần F5 hoặc Polling
                setNotifications(prev => {
                    // Tránh trùng lặp nếu trùng ID
                    if (prev.some(item => item.id === formattedNotif.id)) {
                        return prev;
                    }
                    return [formattedNotif, ...prev];
                });

            } catch (err) {
                console.error("[SSE] Lỗi parse dữ liệu thông báo:", err);
            }
        });

        eventSource.onerror = (error) => {
            console.warn("[SSE] Kết nối bị ngắt, trình duyệt tự động thử lại kết nối...", error);
        };

        return () => {
            console.log("[SSE] Đang đóng kết nối SSE...");
            eventSource.close();
        };
    }, []);

    // Helper log đơn giản
    function logSseDebug(msg) {
        console.log(`[SSE Debug] ${msg}`);
    }

    // Đánh dấu 1 thông báo đã đọc
    const markAsRead = async (id) => {
        try {
            await userNotificationService.markAsRead(id);
            setNotifications(prev => 
                prev.map(item => item.id === id ? { ...item, isRead: true, read: true } : item)
            );
        } catch (error) {
            console.error('Failed to mark notification as read:', error);
        }
    };

    // Đánh dấu tất cả đã đọc
    const markAllAsRead = async () => {
        try {
            await userNotificationService.markAllAsRead();
            setNotifications(prev => 
                prev.map(item => (item.isRead || item.read) ? item : { ...item, isRead: true, read: true })
            );
        } catch (error) {
            console.error('Failed to mark all as read:', error);
        }
    };

    const handleSelectNotification = (notification) => {
        setSelectedNotification(notification);
        if (!(notification.isRead || notification.read)) {
            markAsRead(notification.id);
        }
    };

    return {
        notifications,
        loading,
        unreadCount,
        selectedNotification,
        setSelectedNotification,
        markAsRead,
        markAllAsRead,
        handleSelectNotification,
        refresh: fetchNotifications
    };
};
```
