package com.example.demo.modules.alert.services;

import com.example.demo.common.exceptions.UserNotFoundException;
import com.example.demo.modules.alert.dto.AdminUserMonitorAlertResponse;
import com.example.demo.modules.alert.dto.AlertSummaryResponse;
import com.example.demo.modules.alert.repositories.AlertDeliveryRepository;
import com.example.demo.modules.alert.repositories.IncidentRepository;
import com.example.demo.modules.user.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AlertSummaryService implements IAlertSummaryService {

        private final IncidentRepository incidentRepository;
        private final AlertDeliveryRepository alertDeliveryRepository;
        private final UserRepository userRepository;

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
                if (range == null)
                        return LocalDateTime.now().minusHours(24);
                return switch (range.toLowerCase()) {
                        case "7d" -> LocalDateTime.now().minusDays(7);
                        case "30d" -> LocalDateTime.now().minusDays(30);
                        default -> LocalDateTime.now().minusHours(24);
                };
        }

        @Override
        // @Cacheable(value = "alerts:admin:summary", key = "#userId + ':' +
        // T(java.time.YearMonth).now().toString()")
        public AdminUserMonitorAlertResponse countAlertsAndIncidentsInMonth(UUID userId) {
                if (!userRepository.existsById(userId)) {
                        throw new UserNotFoundException("User not found: " + userId);
                }
                YearMonth currentMonth = YearMonth.now();
                LocalDateTime startMonth = currentMonth.atDay(1).atStartOfDay();
                LocalDateTime endMonth = currentMonth.atEndOfMonth().atTime(LocalTime.MAX); // Ra 23:59:59.9

                long totalAlert = alertDeliveryRepository.countAlertDeliveryInDateRange(userId, startMonth, endMonth);
                long totalIncident = incidentRepository.countIncidentInDateRange(userId, startMonth, endMonth);

                return AdminUserMonitorAlertResponse.builder()
                                .totalAlert(totalAlert)
                                .totalIncident(totalIncident)
                                .build();
        }
}
