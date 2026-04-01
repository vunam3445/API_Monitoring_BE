package com.example.demo.modules.uptimeLogs.repositories;

import com.example.demo.modules.uptimeLogs.entities.UptimeLogs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.demo.modules.monitor.dto.MonitoringChartProjection;
import java.util.List;

public interface UptimeLogsRepository extends JpaRepository<UptimeLogs, Long>, JpaSpecificationExecutor<UptimeLogs> {

    /**
     * Tìm uptime logs theo monitorId, sắp xếp theo thời gian mới nhất (phân trang).
     */
    Page<UptimeLogs> findByMonitorIdOrderByCheckedAtDesc(UUID monitorId, Pageable pageable);
 
    /**
     * Tìm uptime logs theo userId (tất cả monitor của user), sắp xếp theo thời gian mới nhất.
     */
    Page<UptimeLogs> findByMonitorUserIdOrderByCheckedAtDesc(UUID userId, Pageable pageable);

    /**
     * Tìm uptime logs theo monitorId trong khoảng thời gian (cho biểu đồ).
     */
    Page<UptimeLogs> findByMonitorIdAndCheckedAtBetweenOrderByCheckedAtDesc(
            UUID monitorId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    /**
     * Đếm số lần check thành công (isUp = true) theo monitorId.
     * Dùng để tính uptime percentage.
     */
    long countByMonitorIdAndIsUp(UUID monitorId, Boolean isUp);

    /**
     * Đếm tổng số lần check theo monitorId.
     */
    long countByMonitorId(UUID monitorId);

    @Query("SELECT COUNT(l) FROM UptimeLogs l WHERE l.monitor.userId = :userId AND l.checkedAt >= :since")
    long countTotalByUser(@Param("userId") UUID userId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(l) FROM UptimeLogs l WHERE l.monitor.userId = :userId AND l.isUp = true AND l.checkedAt >= :since")
    long countUpByUser(@Param("userId") UUID userId, @Param("since") LocalDateTime since);
 
    @Query(value = "SELECT " +
           "date_trunc(:bucket, l.checked_at) AS time, " +
           "AVG(l.response_time_ms) AS avgResponseTimeMs, " +
           "COUNT(*) AS totalChecks, " +
           "COUNT(CASE WHEN l.is_up = false THEN 1 END) AS failedChecks " +
           "FROM uptime_logs l " +
           "JOIN monitors m ON l.monitor_id = m.id " +
           "WHERE m.user_id = :userId " +
           "AND (CAST(:monitorId AS UUID) IS NULL OR l.monitor_id = CAST(:monitorId AS UUID)) " +
           "AND l.checked_at >= :since " +
           "GROUP BY time ORDER BY time ASC",
           nativeQuery = true)
    List<MonitoringChartProjection> getAggregatedUptimeLogs(
            @Param("userId") UUID userId,
            @Param("monitorId") Object monitorId,
            @Param("since") LocalDateTime since,
            @Param("bucket") String bucket);
 
    @Query(value = "SELECT " +
           "to_timestamp(floor(extract(epoch from l.checked_at) / :bucketSeconds) * :bucketSeconds) AS time, " +
           "CAST(AVG(l.response_time_ms) AS INTEGER) AS latencyMs, " +
           "CASE WHEN COUNT(CASE WHEN l.is_up = false THEN 1 END) > 0 THEN false ELSE true END AS isUp " +
           "FROM uptime_logs l " +
           "WHERE l.monitor_id = CAST(:monitorId AS UUID) " +
           "AND l.checked_at >= :since " +
           "GROUP BY time ORDER BY time ASC",
           nativeQuery = true)
    List<com.example.demo.modules.monitor.dto.UptimeStatusProjection> getUptimeStatusBuckets(
            @Param("monitorId") Object monitorId,
            @Param("since") LocalDateTime since,
            @Param("bucketSeconds") int bucketSeconds);
 
    @Query(value = "SELECT response_time_ms FROM uptime_logs WHERE monitor_id = CAST(:monitorId AS UUID) ORDER BY checked_at DESC LIMIT :limit", nativeQuery = true)
    List<Integer> findRecentLatency(@Param("monitorId") Object monitorId, @Param("limit") int limit);
 
    @Query("SELECT COUNT(l) FROM UptimeLogs l WHERE l.monitor.id = :monitorId AND l.checkedAt >= :since")
    long countByMonitorIdAndSince(@Param("monitorId") UUID monitorId, @Param("since") LocalDateTime since);
 
    @Query("SELECT COUNT(l) FROM UptimeLogs l WHERE l.monitor.id = :monitorId AND l.isUp = :isUp AND l.checkedAt >= :since")
    long countByMonitorIdAndIsUpAndSince(@Param("monitorId") UUID monitorId, @Param("isUp") Boolean isUp, @Param("since") LocalDateTime since);
}

