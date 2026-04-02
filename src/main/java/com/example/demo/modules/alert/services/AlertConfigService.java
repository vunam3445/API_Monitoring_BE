package com.example.demo.modules.alert.services;

import com.example.demo.common.cache.ICacheService;
import com.example.demo.common.exceptions.ResourceNotFoundException;
import com.example.demo.modules.alert.dto.AlertConfigResponse;
import com.example.demo.modules.alert.dto.CreateAlertConfigRequest;
import com.example.demo.modules.alert.entities.AlertConfig;
import com.example.demo.modules.alert.mappers.AlertMapper;
import com.example.demo.modules.alert.repositories.AlertConfigRepository;
import com.example.demo.modules.monitor.entities.Monitor;
import com.example.demo.modules.monitor.repositories.MonitorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertConfigService implements IAlertConfigService {

    private final AlertConfigRepository alertConfigRepository;
    private final MonitorRepository monitorRepository;
    private final AlertMapper mapper;
    private final ICacheService cacheService;

    @Override
    public List<AlertConfigResponse> findAllByMonitorId(UUID monitorId) {
        List<AlertConfig> entities = alertConfigRepository.findAllByMonitorId(monitorId);
        return mapper.toConfigResponseList(entities);
    }

    @Override
    @Transactional
    public AlertConfigResponse create(UUID monitorId, CreateAlertConfigRequest request) {
        Monitor monitor = monitorRepository.findById(monitorId)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found: " + monitorId));
        
        AlertConfig entity = AlertConfig.builder()
                .monitor(monitor)
                .type(request.getType())
                .destination(request.getDestination())
                .isEnabled(request.getIsEnabled() != null ? request.getIsEnabled() : true)
                .build();
                
        AlertConfig saved = alertConfigRepository.save(entity);
        
        // Invalidate monitor detail cache as it might show configs
        cacheService.evict("monitoring:overview:" + monitorId);
        
        return mapper.toConfigResponse(saved);
    }

    @Override
    @Transactional
    public AlertConfigResponse update(UUID id, CreateAlertConfigRequest request) {
        AlertConfig entity = alertConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert config not found: " + id));
        
        entity.setType(request.getType());
        entity.setDestination(request.getDestination());
        if (request.getIsEnabled() != null) {
            entity.setIsEnabled(request.getIsEnabled());
        }
        
        AlertConfig saved = alertConfigRepository.save(entity);
        cacheService.evict("monitoring:overview:" + saved.getMonitor().getId());
        
        return mapper.toConfigResponse(saved);
    }

    @Override
    @Transactional
    public void toggle(UUID id) {
        AlertConfig entity = alertConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert config not found: " + id));
        
        entity.setIsEnabled(!entity.getIsEnabled());
        alertConfigRepository.save(entity);
        cacheService.evict("monitoring:overview:" + entity.getMonitor().getId());
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        AlertConfig entity = alertConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert config not found: " + id));
        
        UUID monitorId = entity.getMonitor().getId();
        alertConfigRepository.delete(entity);
        cacheService.evict("monitoring:overview:" + monitorId);
    }
}
