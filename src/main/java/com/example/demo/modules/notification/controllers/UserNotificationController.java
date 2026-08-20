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

/**
 * REST API dành cho User cá nhân và kết nối thời gian thực SSE.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class UserNotificationController {

    private final NotificationService notificationService;
    private final NotificationSseService notificationSseService;
    private final ISecurityContextService securityContextService;

    /**
     * Mở kết nối SSE trực tuyến (streaming) bảo mật với Header Authorization.
     */
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
