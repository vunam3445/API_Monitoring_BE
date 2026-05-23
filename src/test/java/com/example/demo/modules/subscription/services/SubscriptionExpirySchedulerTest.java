package com.example.demo.modules.subscription.services;

import com.example.demo.modules.subscription.config.SubscriptionExpiryMQConfig;
import com.example.demo.modules.subscription.dto.SubscriptionExpiryEvent;
import com.example.demo.modules.subscription.entities.Subscription;
import com.example.demo.modules.subscription.entities.SubscriptionPlan;
import com.example.demo.modules.subscription.enums.SubscriptionStatus;
import com.example.demo.modules.subscription.repositories.SubscriptionRepository;
import com.example.demo.modules.user.entities.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SubscriptionExpirySchedulerTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ISubscriptionService subscriptionService;

    @InjectMocks
    private SubscriptionExpiryScheduler scheduler;

    @Test
    public void testScanAndNotifyExpiringSubscriptions_Success() {
        // Arrange
        User user = new User();
        user.setEmail("test@example.com");
        user.setFullName("Nguyen Van A");

        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setName("PRO PLAN");
        plan.setPrice(BigDecimal.valueOf(199000));

        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setUser(user);
        subscription.setPlan(plan);
        subscription.setCurrentPeriodEnd(LocalDateTime.now().plusDays(3));

        when(subscriptionRepository.findActivePaidSubscriptionsExpiringBetween(
                eq(SubscriptionStatus.ACTIVE), any(), any()))
                .thenReturn(Collections.singletonList(subscription));

        // Act
        scheduler.scanAndNotifyExpiringSubscriptions();

        // Assert
        ArgumentCaptor<SubscriptionExpiryEvent> eventCaptor = ArgumentCaptor.forClass(SubscriptionExpiryEvent.class);
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(SubscriptionExpiryMQConfig.EXPIRY_QUEUE), eventCaptor.capture());

        SubscriptionExpiryEvent capturedEvent = eventCaptor.getValue();
        assertEquals("test@example.com", capturedEvent.userEmail());
        assertEquals("Nguyen Van A", capturedEvent.userName());
        assertEquals("PRO PLAN", capturedEvent.planName());
    }

    @Test
    public void testScanAndDowngradeExpiredSubscriptions_Success() {
        // Arrange
        User user = new User();
        user.setEmail("expired@example.com");
        user.setFullName("Expired User");

        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setName("PRO PLAN");
        plan.setPrice(BigDecimal.valueOf(199000));

        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setUser(user);
        subscription.setPlan(plan);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodEnd(LocalDateTime.now().minusMinutes(5));

        when(subscriptionRepository.findActivePaidSubscriptionsExpiringBefore(
                eq(SubscriptionStatus.ACTIVE), any()))
                .thenReturn(Collections.singletonList(subscription));

        // Act
        scheduler.scanAndDowngradeExpiredSubscriptions();

        // Assert
        assertEquals(SubscriptionStatus.EXPIRED, subscription.getStatus());
        verify(subscriptionRepository, times(1)).save(subscription);
        verify(subscriptionService, times(1)).subscribeFreePlan(user);
    }
}
