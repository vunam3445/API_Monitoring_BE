package com.example.demo.modules.subscription.controllers;

import com.example.demo.common.base.BaseController;

import com.example.demo.modules.subscription.dto.CreatePlanRequest;
import com.example.demo.modules.subscription.dto.UpdatePlanRequest;
import com.example.demo.modules.subscription.dto.PlanResponse;
import com.example.demo.common.security.annotations.IsAdmin;
import com.example.demo.common.security.annotations.IsAuthenticated;
import com.example.demo.modules.subscription.services.ISubscriptionPlanService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/subscription-plans")
@IsAdmin
public class SubscriptionPlanController
        extends BaseController<CreatePlanRequest, UpdatePlanRequest, PlanResponse, UUID> {

    public SubscriptionPlanController(ISubscriptionPlanService service) {
        super(service);
    }

    @Override
    @IsAuthenticated
    public ResponseEntity<Page<PlanResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @Override
    @IsAuthenticated
    public ResponseEntity<PlanResponse> getById(@PathVariable UUID id) {
        return super.getById(id);
    }

    @GetMapping("/user/{userId}")
    @IsAuthenticated
    public ResponseEntity<java.util.List<PlanResponse>> getAllWithUserStatus(@PathVariable UUID userId) {
        return ResponseEntity.ok(((ISubscriptionPlanService)service).findAllWithUserStatus(userId));
    }
   
    @GetMapping
    @IsAuthenticated
    public ResponseEntity<java.util.List<PlanResponse>> getAllUnpaged() {
        return ResponseEntity.ok(((ISubscriptionPlanService)service).findAllPlans());
    }
}
