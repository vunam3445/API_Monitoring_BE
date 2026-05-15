package com.example.demo.modules.alert.repositories;

import com.example.demo.modules.alert.entities.AlertDelivery;
import com.example.demo.modules.alert.enums.AlertDeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AlertDeliveryRepository extends JpaRepository<AlertDelivery, UUID> {
    List<AlertDelivery> findAllByIncidentIdOrderByCreatedAtDesc(UUID incidentId);

    long countByIncidentIdAndStatus(UUID incidentId, AlertDeliveryStatus status);

    @Query("SELECT COUNT(a) FROM AlertDelivery a "
            + "JOIN a.incident i "
            + "JOIN i.monitor m "
            + "WHERE m.userId = :userId "
            + "AND a.sentAt >= :startDate "
            + "AND a.sentAt <= :endDate ")
    long countAlertDeliveryInDateRange(@Param("userId") UUID userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    long countByStatus(AlertDeliveryStatus status);

    long countByStatusAndCreatedAtAfter(AlertDeliveryStatus status, LocalDateTime since);

    @Query("SELECT a.errorMessage FROM AlertDelivery a " +
           "WHERE a.status = 'FAILED' AND a.createdAt >= :since " +
           "GROUP BY a.errorMessage ORDER BY COUNT(a) DESC LIMIT 1")
    String findMostCommonError(@Param("since") LocalDateTime since);

    @Query("SELECT AVG(a.latencyMs) FROM AlertDelivery a WHERE a.status = 'SENT' AND a.createdAt >= :since")
    Double getAverageLatency(@Param("since") LocalDateTime since);

    @Query("SELECT a FROM AlertDelivery a " +
           "JOIN a.incident i " +
           "JOIN i.monitor m " +
           "WHERE (:channel IS NULL OR a.channel = :channel) " +
           "AND (:status IS NULL OR a.status = :status) " +
           "AND (:search IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :search, '%')) OR CAST(a.id AS string) LIKE CONCAT('%', :search, '%'))")
    org.springframework.data.domain.Page<AlertDelivery> findWithFilters(
            @Param("channel") com.example.demo.modules.alert.enums.AlertChannelType channel,
            @Param("status") AlertDeliveryStatus status,
            @Param("search") String search,
            org.springframework.data.domain.Pageable pageable);

    List<AlertDelivery> findAllByStatusAndCreatedAtAfter(AlertDeliveryStatus status, LocalDateTime since);
}
