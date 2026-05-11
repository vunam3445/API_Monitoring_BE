package com.example.demo.modules.monitor.services;

import com.example.demo.modules.monitor.dto.MonitorFilterCriteria;
import com.example.demo.modules.monitor.repositories.MonitorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
public class ManagerMonitorServiceCachingTest {

    @TestConfiguration
    static class CachingTestConfig {
        @Bean
        @Primary
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("api-monitoring:api:list");
        }
    }

    @Autowired
    private IManagerMonitorService managerMonitorService;

    @MockitoBean
    private MonitorRepository monitorRepository;

    @Test
    public void getAllMonitors_ShouldUseCache() {
        // Arrange
        MonitorFilterCriteria criteria = new MonitorFilterCriteria();
        Pageable pageable = PageRequest.of(0, 10);
        
        when(monitorRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));

        // Act
        // Gọi lần 1
        managerMonitorService.getAllMonitors(criteria, pageable);
        // Gọi lần 2
        managerMonitorService.getAllMonitors(criteria, pageable);

        // Assert
        verify(monitorRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }
}
