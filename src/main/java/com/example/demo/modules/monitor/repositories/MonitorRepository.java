package com.example.demo.modules.monitor.repositories;

import com.example.demo.modules.monitor.entities.Monitor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MonitorRepository extends JpaRepository<Monitor, UUID>, JpaSpecificationExecutor<Monitor> {
    Page<Monitor> findByUserId(UUID userId, Pageable pageable);

    /**
     * Tìm các monitor đang active và đã đến hạn kiểm tra.
     * Monitor đến hạn khi:
     * - nextCheckAt <= now (đã quá thời gian dự kiến)
     * - HOẶC nextCheckAt IS NULL (chưa chạy lần nào)
     */
    @Query("SELECT m FROM Monitor m WHERE m.isActive = true " +
           "AND (m.nextCheckAt IS NULL OR m.nextCheckAt <= :now)")
    List<Monitor> findDueMonitors(@Param("now") LocalDateTime now);

    long countByUserId(UUID userId);
}
