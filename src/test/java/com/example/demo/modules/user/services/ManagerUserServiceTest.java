package com.example.demo.modules.user.services;

import com.example.demo.common.security.ISecurityContextService;
import com.example.demo.modules.user.entities.User;
import com.example.demo.modules.user.enums.UserStatus;
import com.example.demo.modules.user.mappers.UserMapper;
import com.example.demo.modules.user.repositories.UserRepository;
import com.example.demo.modules.notification.services.NotificationService;
import com.example.demo.common.cache.ICacheService;
import com.example.demo.modules.subscription.services.ISubscriptionService;
import com.example.demo.modules.monitor.repositories.MonitorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ManagerUserServiceTest {

    private UserRepository userRepository;
    private UserMapper mapper;
    private ISecurityContextService securityContextService;
    private ICacheService cacheService;
    private ISubscriptionService subscriptionService;
    private MonitorRepository monitorRepository;
    private NotificationService notificationService;
    private ManagerUserService managerUserService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        mapper = mock(UserMapper.class);
        securityContextService = mock(ISecurityContextService.class);
        cacheService = mock(ICacheService.class);
        subscriptionService = mock(ISubscriptionService.class);
        monitorRepository = mock(MonitorRepository.class);
        notificationService = mock(NotificationService.class);

        managerUserService = new ManagerUserService(
                userRepository,
                mapper,
                securityContextService,
                cacheService,
                subscriptionService,
                monitorRepository,
                notificationService
        );
    }

    @Test
    void blockUser_shouldSuspendUserAndClearRefreshToken() {
        // Arrange
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");
        user.setStatus(UserStatus.ACTIVE);
        user.setRefreshToken("some-refresh-token");
        user.setRefreshTokenExpiry(LocalDateTime.now().plusDays(7));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(securityContextService.getCurrentUserId()).thenReturn(Optional.empty());

        // Act
        managerUserService.blockUser(userId);

        // Assert
        assertEquals(UserStatus.SUSPENDED, user.getStatus());
        assertNull(user.getRefreshToken());
        assertNull(user.getRefreshTokenExpiry());
        verify(userRepository, times(1)).saveAndFlush(user);
    }
}
