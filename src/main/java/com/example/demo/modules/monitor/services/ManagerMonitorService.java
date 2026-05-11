package com.example.demo.modules.monitor.services;

import com.example.demo.modules.monitor.dto.ApiResponse;
import com.example.demo.modules.monitor.dto.MonitorFilterCriteria;
import com.example.demo.modules.monitor.dto.MonitorStatisticsDTO;
import com.example.demo.modules.monitor.entities.Monitor;
import com.example.demo.modules.monitor.mappers.MonitorMapper;
import com.example.demo.modules.monitor.repositories.MonitorRepository;
import com.example.demo.modules.monitor.repositories.MonitorSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import com.example.demo.common.base.RestPageImpl;
import org.springframework.cache.annotation.Cacheable;

import org.springframework.stereotype.Service;


import java.util.List;

@Service
@RequiredArgsConstructor
public class ManagerMonitorService implements IManagerMonitorService {

    private final MonitorRepository monitorRepository;
    private final MonitorMapper mapper;

    @Override
    @Cacheable(value = "api-monitoring:api:list",
            key = "'manager:list:user_' + (#criteria.userId ?: 'all') + " +
                    "':status_' + (#criteria.lastStatus ?: 'all') + " +
                    "':active_' + (#criteria.isActive ?: 'all') + " +
                    "':search_' + (#criteria.search ?: 'none') + " +
                    "':page_' + #pageable.pageNumber + " +
                    "':size_' + #pageable.pageSize + " +
                    "':sort_' + #pageable.sort.toString().replaceAll(':', '_')",
            unless = "#result == null")
    public Page<ApiResponse> getAllMonitors(MonitorFilterCriteria criteria, Pageable pageable) {

        Specification<Monitor> spec = (root, query, cb) -> cb.conjunction();

        if (criteria.getUserId() != null) {
            spec = spec.and(MonitorSpecification.hasUserId(criteria.getUserId()));
        }

        if (criteria.getLastStatus() != null && !criteria.getLastStatus().isBlank()) {
            spec = spec.and(MonitorSpecification.hasStatus(criteria.getLastStatus()));
        }

        if (criteria.getIsActive() != null) {
            spec = spec.and(MonitorSpecification.hasActive(criteria.getIsActive()));
        }

        if (criteria.getSearch() != null && !criteria.getSearch().isBlank()) {
            spec = spec.and(MonitorSpecification.hasNameLike(criteria.getSearch()));
        }

        Page<Monitor> entityPage = monitorRepository.findAll(spec, pageable);
        
        Page<ApiResponse> dtoPage = entityPage.map(mapper::toResponse);

        return new RestPageImpl<>(
                dtoPage.getContent(),
                dtoPage.getNumber(),
                dtoPage.getSize(),
                dtoPage.getTotalElements(),
                null, false, 0, null, false, 0);
    }

    public MonitorStatisticsDTO getMonitorStatistics() {
        Object[] result = monitorRepository.countGlobalMonitorStats();
        
        // Kiểm tra nếu kết quả trả về bị lồng trong một mảng khác (thường gặp trong Hibernate aggregate query)
        Object[] row = (result != null && result.length > 0 && result[0] instanceof Object[]) 
                ? (Object[]) result[0] 
                : result;

        MonitorStatisticsDTO dto = new MonitorStatisticsDTO();
        if (row != null && row.length >= 3) {
            dto.setTotalMonitors(row[0] != null ? ((Number) row[0]).longValue() : 0);
            dto.setActiveMonitors(row[1] != null ? ((Number) row[1]).longValue() : 0);
            dto.setDownMonitors(row[2] != null ? ((Number) row[2]).longValue() : 0);
        }
        return dto;
    }
}
