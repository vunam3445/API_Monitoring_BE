package com.example.demo.repositories.subscriptionPlan;

import com.example.demo.entities.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, UUID> {
    Optional<SubscriptionPlan> findByName(String name);
    Optional<SubscriptionPlan> findById(UUID Id);
}
