package com.example.demo.modules.alert.repositories;

import com.example.demo.modules.alert.entities.AlertDelivery;
import com.example.demo.modules.alert.enums.AlertDeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlertDeliveryRepository extends JpaRepository<AlertDelivery, UUID> {
    List<AlertDelivery> findAllByIncidentIdOrderByCreatedAtDesc(UUID incidentId);
    long countByIncidentIdAndStatus(UUID incidentId, AlertDeliveryStatus status);
}
