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
}
