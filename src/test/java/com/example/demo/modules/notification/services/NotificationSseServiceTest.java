package com.example.demo.modules.notification.services;

import com.example.demo.modules.notification.dto.SseNotificationPayload;
import com.example.demo.modules.notification.dto.UserNotificationResponse;
import com.example.demo.common.config.RedisPubSubConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationSseServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    private NotificationSseServiceImpl sseService;

    @BeforeEach
    void setUp() {
        sseService = new NotificationSseServiceImpl(redisTemplate);
    }

    @Test
    void testSendNotification_PublishesToRedis() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserNotificationResponse response = UserNotificationResponse.builder()
                .title("Test alert")
                .content("API Down")
                .build();

        // Act
        sseService.sendNotification(userId, response);

        // Assert
        ArgumentCaptor<SseNotificationPayload> payloadCaptor = ArgumentCaptor.forClass(SseNotificationPayload.class);
        verify(redisTemplate, times(1)).convertAndSend(
                eq(RedisPubSubConfig.SSE_CHANNEL), payloadCaptor.capture());
        
        SseNotificationPayload captured = payloadCaptor.getValue();
        assertEquals(userId, captured.getUserId());
        assertEquals("Test alert", captured.getNotification().getTitle());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSendNotificationLocal_DeliversToActiveEmitter() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserNotificationResponse response = UserNotificationResponse.builder()
                .title("Local alert")
                .content("API recovered")
                .build();

        // mock emitter
        SseEmitter emitter = mock(SseEmitter.class);
        
        // inject emitter to service emitters map via reflection
        Field emittersField = NotificationSseServiceImpl.class.getDeclaredField("emitters");
        emittersField.setAccessible(true);
        Map<UUID, SseEmitter> emitters = (Map<UUID, SseEmitter>) emittersField.get(sseService);
        emitters.put(userId, emitter);

        // Act
        sseService.sendNotificationLocal(userId, response);

        // Assert
        verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }
}
