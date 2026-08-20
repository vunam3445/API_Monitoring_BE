package com.example.demo.modules.notification.repositories;

import com.example.demo.modules.notification.entities.UserNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserNotificationRepository extends JpaRepository<UserNotification, UUID> {

    /** Lấy tất cả thông báo của user, sắp theo mới nhất */
    Page<UserNotification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /** Lấy thông báo chưa đọc của user */
    Page<UserNotification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /** Đếm số thông báo chưa đọc */
    long countByUserIdAndIsReadFalse(UUID userId);

    /** Tìm một bản ghi cụ thể theo id và userId (tránh user đọc của người khác) */
    Optional<UserNotification> findByIdAndUserId(UUID id, UUID userId);

    /** Đánh dấu tất cả thông báo của user là đã đọc */
    @Modifying
    @Query("UPDATE UserNotification un SET un.isRead = true, un.readAt = CURRENT_TIMESTAMP " +
           "WHERE un.user.id = :userId AND un.isRead = false")
    int markAllAsRead(@Param("userId") UUID userId);
}
