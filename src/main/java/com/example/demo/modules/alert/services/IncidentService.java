package com.example.demo.modules.alert.services;

import com.example.demo.common.cache.ICacheService;
import com.example.demo.modules.alert.entities.Incident;
import com.example.demo.modules.alert.enums.IncidentStatus;
import com.example.demo.modules.alert.repositories.IncidentRepository;
import com.example.demo.modules.monitor.entities.Monitor;
import com.example.demo.modules.uptimeLogs.entities.UptimeLogs;
import com.example.demo.modules.user.repositories.UserSettingRepository;
import com.example.demo.modules.dashboard.services.DashboardCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentService implements IIncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentRuleEvaluator ruleEvaluator;
    private final ICacheService cacheService;
    private final AlertNotificationDispatcher notificationDispatcher;
    private final UserSettingRepository userSettingRepository;
    private final DashboardCacheService dashboardCacheService;

    @Override
    @Transactional
    public Optional<Incident> processCheckResult(Monitor monitor, UptimeLogs logResult) {
        // 1. Phân tích kết quả
        Optional<IncidentRuleEvaluator.EvaluationResult> evalResult = ruleEvaluator.evaluate(monitor, logResult);

        if (evalResult.isEmpty()) {
            // Không có lỗi/warning gì hoặc monitor đang UP
            resolveActiveIncidents(monitor, logResult);
            return Optional.empty();
        }

        IncidentRuleEvaluator.EvaluationResult res = evalResult.get();

        // 2. Tìm incident active cùng loại cho monitor này
        Optional<Incident> activeIncident = incidentRepository.findActiveIncident(
                monitor.getId(),
                res.getType(),
                Arrays.asList(IncidentStatus.ACTIVE, IncidentStatus.ACKNOWLEDGED));

        Incident incident;
        if (activeIncident.isPresent()) {
            // Update incident hiện có (Keep existing one active but update last_seen_at)
            incident = activeIncident.get();
            incident.setLastSeenAt(logResult.getCheckedAt());
            incident.setConsecutiveFailCount(incident.getConsecutiveFailCount() + 1);
            incident.setAvgLatencyMs(
                    logResult.getResponseTimeMs() != null ? logResult.getResponseTimeMs().longValue() : null);
            incident.setLastStatusCode(logResult.getStatusCode());
            incident.setMessage(res.getMessage());
            log.debug("Updated active incident {} for monitor {}", incident.getId(), monitor.getName());
        } else {
            // Tạo mới incident
            incident = Incident.builder()
                    .monitor(monitor)
                    .type(res.getType())
                    .severity(res.getSeverity())
                    .status(IncidentStatus.ACTIVE)
                    .title(res.getTitle())
                    .message(res.getMessage())
                    .triggeredAt(logResult.getCheckedAt())
                    .lastSeenAt(logResult.getCheckedAt())
                    .consecutiveFailCount(1)
                    .avgLatencyMs(
                            logResult.getResponseTimeMs() != null ? logResult.getResponseTimeMs().longValue() : null)
                    .lastStatusCode(logResult.getStatusCode())
                    .build();
            log.info("Issue detected for monitor {}: {}", monitor.getName(), incident.getTitle());
        }

        Incident saved = incidentRepository.save(incident);

        // 3. Invalidate cache
        invalidateCache(monitor.getUserId(), saved.getId());

        // 4. Trigger notification (Optimized: only if critical change or
        // re-notification interval reached)
        boolean notify = shouldNotify(saved, activeIncident.isEmpty());
        log.info("Incident {}: shouldNotify={}", saved.getId(), notify);

        if (notify) {
            if (!org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
                log.warn("Transaction synchronization is NOT active. Falling back to immediate dispatch.");
                notificationDispatcher.dispatch(saved);
            } else {
                log.info("Registering afterCommit synchronization for incident {}", saved.getId());
                org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            log.info("Transaction COMMITTED. Dispatching notification for incident {}", saved.getId());
                            notificationDispatcher.dispatch(saved);
                        }
                    }
                );
            }
            saved.setLastNotifiedAt(LocalDateTime.now());
            incidentRepository.save(saved);
        }

        return Optional.of(saved);
    }

    /**
     * Kiểm tra xem có nên gửi thông báo cho user không.
     * Tránh gửi liên tục mỗi 1-2 phút (Alert Fatigue).
     */
    private boolean shouldNotify(Incident incident, boolean isNew) {
        if (isNew) {
            log.info("shouldNotify: true (New Incident)");
            return true;
        }

        if (incident.getStatus() == IncidentStatus.ACKNOWLEDGED) {
            log.info("shouldNotify: false (Acknowledged)");
            return false;
        }

        if (incident.getStatus() == IncidentStatus.ACTIVE) {
            if (incident.getLastNotifiedAt() == null) {
                log.info("shouldNotify: true (No lastNotifiedAt)");
                return true;
            }

            var setting = userSettingRepository.findById(incident.getMonitor().getUserId()).orElse(null);
            int interval = (setting != null && setting.getReNotificationIntervalMinutes() != null)
                    ? setting.getReNotificationIntervalMinutes() : 60;

            LocalDateTime nextNotify = incident.getLastNotifiedAt().plusMinutes(interval);
            boolean ready = LocalDateTime.now().isAfter(nextNotify);
            log.info("shouldNotify: check interval. Last={}, Next={}, Ready={}", incident.getLastNotifiedAt(), nextNotify, ready);
            return ready;
        }

        return false;
    }

    @Override
    @Transactional
    public void resolveActiveIncidents(Monitor monitor, UptimeLogs logResult) {
        // Tìm tất cả incident đang active/acknowledged của monitor này
        List<Incident> actives = incidentRepository.findAllByMonitorIdAndStatusIn(
                monitor.getId(),
                Arrays.asList(IncidentStatus.ACTIVE, IncidentStatus.ACKNOWLEDGED));

        if (actives.isEmpty())
            return;

        LocalDateTime now = LocalDateTime.now();
        actives.forEach(i -> {
            i.setStatus(IncidentStatus.RESOLVED);
            i.setResolvedAt(logResult.getCheckedAt() != null ? logResult.getCheckedAt() : now);
            log.info("Incident resolved: monitor {} recovered from {} at {}", monitor.getName(), i.getType(),
                    i.getResolvedAt());

            // Trigger recovery notification
            if (!org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
                log.warn("Transaction synchronization NOT active for resolution. immediate dispatch.");
                notificationDispatcher.dispatch(i);
            } else {
                org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            log.info("Transaction COMMITTED. Dispatching recovery notification for incident {}", i.getId());
                            notificationDispatcher.dispatch(i);
                        }
                    }
                );
            }
        });

        incidentRepository.saveAll(actives);

        // Invalidate cache cho user
        invalidateCache(monitor.getUserId(), null);
    }

    @Override
    @Transactional
    public void acknowledge(UUID userId, UUID id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new com.example.demo.common.exceptions.ResourceNotFoundException(
                        "Incident không tồn tại: " + id));

        if (!incident.getMonitor().getUserId().equals(userId)) {
            throw new com.example.demo.common.exceptions.ForbidenException("Bạn không có quyền xác nhận alert này.");
        }

        if (incident.getStatus() != IncidentStatus.ACTIVE) {
            return;
        }

        incident.setStatus(IncidentStatus.ACKNOWLEDGED);
        incidentRepository.save(incident);
        invalidateCache(userId, id);
    }

    @Override
    @Transactional
    public void resolve(UUID userId, UUID id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new com.example.demo.common.exceptions.ResourceNotFoundException(
                        "Incident không tồn tại: " + id));

        if (!incident.getMonitor().getUserId().equals(userId)) {
            throw new com.example.demo.common.exceptions.ForbidenException("Bạn không có quyền đóng alert này.");
        }

        if (incident.getStatus() == IncidentStatus.RESOLVED)
            return;

        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setResolvedAt(LocalDateTime.now());
        incidentRepository.save(incident);
        invalidateCache(userId, id);
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new com.example.demo.common.exceptions.ResourceNotFoundException(
                        "Incident không tồn tại: " + id));

        if (!incident.getMonitor().getUserId().equals(userId)) {
            throw new com.example.demo.common.exceptions.ForbidenException("Bạn không có quyền xóa alert này.");
        }

        incidentRepository.delete(incident);
        invalidateCache(userId, id);
    }

    @Override
    public Optional<Incident> findById(UUID id) {
        return incidentRepository.findById(id);
    }

    private void invalidateCache(UUID userId, UUID incidentId) {
        // Spring Cache mặc định dùng separator :: giữa cacheName và key
        cacheService.evictByPrefix("alerts:summary::" + userId);
        cacheService.evictByPrefix("alerts:list::" + userId);
        if (incidentId != null) {
            cacheService.evict("alerts:detail::" + incidentId);
        }
        dashboardCacheService.clearUserDashboardCache(userId);
    }
}
