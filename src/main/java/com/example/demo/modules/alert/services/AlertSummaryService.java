package com.example.demo.modules.alert.services;

import com.example.demo.modules.alert.dto.AlertSummaryResponse;
import com.example.demo.modules.alert.repositories.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AlertSummaryService {

    private final IncidentRepository incidentRepository;

    @org.springframework.cache.annotation.Cacheable(value = "alerts:summary", key = "#userId + ':' + (#range ?: '24h')")
    public AlertSummaryResponse getSummary(UUID userId, String range) {
        LocalDateTime since = calculateSince(range);
        
        long total = incidentRepository.countTotalAlerts(userId, since);
        long active = incidentRepository.countActiveAlerts(userId);
        long critical = incidentRepository.countCriticalAlerts(userId);
        long resolved = incidentRepository.countResolvedAlerts(userId, since);
        
        double successRate = total > 0 ? (double) resolved * 100 / total : 100.0;
        
        return AlertSummaryResponse.builder()
                .totalAlerts(AlertSummaryResponse.SummaryItem.builder()
                        .value(total)
                        .changePercent(0.0) 
                        .build())
                .activeAlerts(AlertSummaryResponse.ActiveItem.builder()
                        .value(active)
                        .urgentCount(critical) 
                        .build())
                .criticalAlerts(AlertSummaryResponse.CriticalItem.builder()
                        .value(critical)
                        .actionRequired(critical > 0)
                        .build())
                .resolvedAlerts(AlertSummaryResponse.ResolvedItem.builder()
                        .value(resolved)
                        .successRate(Math.round(successRate * 10.0) / 10.0)
                        .build())
                .build();
    }

    private LocalDateTime calculateSince(String range) {
        if (range == null) return LocalDateTime.now().minusHours(24);
        return switch (range.toLowerCase()) {
            case "7d" -> LocalDateTime.now().minusDays(7);
            case "30d" -> LocalDateTime.now().minusDays(30);
            default -> LocalDateTime.now().minusHours(24);
        };
    }
}
