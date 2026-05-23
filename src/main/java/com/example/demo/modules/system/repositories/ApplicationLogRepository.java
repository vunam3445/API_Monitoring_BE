package com.example.demo.modules.system.repositories;

import com.example.demo.modules.system.entities.ApplicationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Repository cho ApplicationLog - log lỗi nội bộ của ứng dụng Backend.
 * KHÔNG liên quan đến UptimeLogsRepository (log giám sát API của user).
 */
@Repository
public interface ApplicationLogRepository
        extends JpaRepository<ApplicationLog, UUID>, JpaSpecificationExecutor<ApplicationLog> {

    @Modifying
    @Query("DELETE FROM ApplicationLog a WHERE a.timestamp < :thresholdTime")
    int deleteLogsOlderThan(@Param("thresholdTime") LocalDateTime thresholdTime);
}
