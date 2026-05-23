package com.example.demo.modules.subscription.services;

import com.example.demo.modules.subscription.dto.ManualRenewalRequest;
import com.example.demo.modules.user.entities.User;

import java.util.UUID;

public interface ISubscriptionService {
    void subscribeFreePlan(User user);
    void updatePlanByAdmin(UUID userId, UUID planId);

    Boolean renewManual(UUID userId, ManualRenewalRequest request);
}
