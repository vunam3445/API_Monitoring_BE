package com.example.demo.modules.alert.repositories;

import com.example.demo.modules.alert.entities.Incident;
import com.example.demo.modules.alert.enums.IncidentSeverity;
import com.example.demo.modules.alert.enums.IncidentStatus;
import com.example.demo.modules.alert.enums.IncidentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID>, JpaSpecificationExecutor<Incident> {
    
    // Tìm incident active theo monitor + type để gộp incident TRÁNH TRÙNG
    @Query("SELECT i FROM Incident i WHERE i.monitor.id = :monitorId AND i.type = :type AND i.status IN :statusList")
    Optional<Incident> findActiveIncident(@Param("monitorId") UUID monitorId, 
                                          @Param("type") IncidentType type, 
                                          @Param("statusList") Collection<IncidentStatus> statusList);

    /**
     * Tìm tất cả alert đang active/acknowledged của 1 monitor để resolve khi nó hồi phục.
     */
    List<Incident> findAllByMonitorIdAndStatusIn(UUID monitorId, Collection<IncidentStatus> statuses);

    // Tìm incident active bất kỳ cho 1 monitor
    Optional<Incident> findFirstByMonitorIdAndStatusInOrderByTriggeredAtDesc(UUID monitorId, Collection<IncidentStatus> statuses);

    // Summary counts
    @Query("SELECT COUNT(i) FROM Incident i WHERE i.monitor.userId = :userId AND i.triggeredAt >= :since")
    long countTotalAlerts(@Param("userId") UUID userId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(i) FROM Incident i WHERE i.monitor.userId = :userId AND i.status != com.example.demo.modules.alert.enums.IncidentStatus.RESOLVED")
    long countActiveAlerts(@Param("userId") UUID userId);

    @Query("SELECT COUNT(i) FROM Incident i WHERE i.monitor.userId = :userId AND i.status != com.example.demo.modules.alert.enums.IncidentStatus.RESOLVED AND i.severity = com.example.demo.modules.alert.enums.IncidentSeverity.CRITICAL")
    long countCriticalAlerts(@Param("userId") UUID userId);

    @Query("SELECT COUNT(i) FROM Incident i WHERE i.monitor.userId = :userId AND i.status = com.example.demo.modules.alert.enums.IncidentStatus.RESOLVED AND i.triggeredAt >= :since")
    long countResolvedAlerts(@Param("userId") UUID userId, @Param("since") LocalDateTime since);

    @Query(value = "SELECT " +
           "i.monitor_id AS monitorId, " +
           "COUNT(*) AS incidentCount, " +
           "SUM(EXTRACT(EPOCH FROM (COALESCE(i.resolved_at, CURRENT_TIMESTAMP) - i.triggered_at)) / 60) AS downtimeMinutes " +
           "FROM incidents i " +
           "JOIN monitors m ON i.monitor_id = m.id " +
           "WHERE m.user_id = :userId " +
           "AND i.triggered_at >= :since " +
           "GROUP BY i.monitor_id",
           nativeQuery = true)
    List<Object[]> getIncidentStats(@Param("userId") UUID userId, @Param("since") LocalDateTime since);
}
