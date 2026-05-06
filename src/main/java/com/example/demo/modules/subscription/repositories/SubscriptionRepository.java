package com.example.demo.modules.subscription.repositories;

import com.example.demo.modules.subscription.entities.Subscription;
import com.example.demo.modules.subscription.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    List<Subscription> findByUserId(UUID userId);

    Optional<Subscription> findByUserIdAndStatus(UUID userId, SubscriptionStatus status);
}
