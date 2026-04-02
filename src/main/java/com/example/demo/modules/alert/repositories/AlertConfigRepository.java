package com.example.demo.modules.alert.repositories;

import com.example.demo.modules.alert.entities.AlertConfig;
import com.example.demo.modules.alert.enums.AlertChannelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlertConfigRepository extends JpaRepository<AlertConfig, UUID> {
    List<AlertConfig> findAllByMonitorId(UUID monitorId);
    List<AlertConfig> findAllByMonitorIdAndIsEnabledTrue(UUID monitorId);
    boolean existsByMonitorIdAndTypeAndIsEnabledTrue(UUID monitorId, AlertChannelType type);
}
