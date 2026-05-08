package com.example.demo.modules.subscription.controllers;

import com.example.demo.common.exceptions.AuthenticationException;
import com.example.demo.common.security.ISecurityContextService;
import com.example.demo.common.security.annotations.IsAdmin;
import com.example.demo.modules.subscription.dto.ManualRenewalRequest;
import com.example.demo.modules.user.entities.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.demo.modules.subscription.services.ISubscriptionService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final ISubscriptionService subscriptionService;
    private final ISecurityContextService securityContextService;

    /**
     * Kích hoạt gói FREE thủ công cho người dùng
     */
    @PostMapping("/free-subscribe")
    public ResponseEntity<?> subscribeFree() {
        User currentUser = securityContextService.getCurrentUser()
                .orElseThrow(() -> new AuthenticationException("Bạn cần đăng nhập để thực hiện hành động này."));

        subscriptionService.subscribeFreePlan(currentUser);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Kích hoạt gói FREE thành công."
        ));
    }

    @IsAdmin
    @PostMapping("/users/{userId}/manual")
    public ResponseEntity<Boolean> manualSubscriptionPlan(@PathVariable UUID userId, @Valid @RequestBody ManualRenewalRequest request) {
        Boolean result = subscriptionService.renewManual(userId, request);
        return ResponseEntity.ok(result);
    }
}
