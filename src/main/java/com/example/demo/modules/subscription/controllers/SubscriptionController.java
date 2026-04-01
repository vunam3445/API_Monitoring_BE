package com.example.demo.modules.subscription.controllers;

import com.example.demo.common.exceptions.AuthenticationException;
import com.example.demo.common.security.ISecurityContextService;
import com.example.demo.modules.subscription.services.SubscriptionService;
import com.example.demo.modules.user.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
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
}
