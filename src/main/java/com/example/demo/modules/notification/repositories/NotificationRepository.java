package com.example.demo.modules.notification.repositories;

import com.example.demo.modules.notification.entities.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
