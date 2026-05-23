package com.example.demo.modules.subscription.dto;

import java.io.Serializable;

public record SubscriptionExpiryEvent(
    String subscriptionId,
    String userEmail,
    String userName,
    String planName,
    String expiryDate
) implements Serializable {}
