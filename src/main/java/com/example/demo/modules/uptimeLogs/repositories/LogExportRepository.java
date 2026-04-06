package com.example.demo.modules.uptimeLogs.repositories;

import com.example.demo.modules.uptimeLogs.entities.UptimeLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.QueryHint;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE;

public interface LogExportRepository extends JpaRepository<UptimeLogs, Long> {

    /**
     * Dùng Stream để đọc tệp lớn không làm tràn RAM.
     * HINT_FETCH_SIZE = 1000: Yêu cầu JDBC Driver chỉ lấy từ DB lên RAM 1000
     * records mỗi lần.
     * org.hibernate.readOnly = true: Không đưa các entity này vào Hibernate Session
     * Context
     * (bỏ qua dirty checking) để tối ưu hiệu suất và bộ nhớ.
     */
    @QueryHints(value = {
            @QueryHint(name = HINT_FETCH_SIZE, value = "1000"),
            @QueryHint(name = "org.hibernate.readOnly", value = "true")
    })
    @Query("SELECT l FROM UptimeLogs l WHERE l.monitor.userId = :userId AND l.checkedAt >= :startDate AND l.checkedAt <= :endDate ORDER BY l.checkedAt DESC")
    Stream<UptimeLogs> streamLogsForExport(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
