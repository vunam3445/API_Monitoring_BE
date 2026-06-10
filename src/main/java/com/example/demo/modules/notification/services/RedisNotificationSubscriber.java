package com.example.demo.modules.notification.services;

import com.example.demo.modules.notification.dto.SseNotificationPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisNotificationSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final NotificationSseService notificationSseService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            SseNotificationPayload payload = objectMapper.readValue(message.getBody(), SseNotificationPayload.class);
            log.info("[RedisPubSub] Received SSE notification event for userId={}", payload.getUserId());
            notificationSseService.sendNotificationLocal(payload.getUserId(), payload.getNotification());
        } catch (IOException e) {
            log.error("[RedisPubSub] Failed to deserialize message body: {}", e.getMessage());
        } catch (Exception e) {
            log.error("[RedisPubSub] Error processing Redis message: {}", e.getMessage());
        }
    }
}
