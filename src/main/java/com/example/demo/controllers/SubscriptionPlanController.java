package com.example.demo.controllers;

import com.example.demo.dto.request.SubscriptionPlan.CreatePlanRequest;
import com.example.demo.dto.request.SubscriptionPlan.UpdatePlanRequest;
import com.example.demo.dto.response.SubscriptionPlan.PlanResponse;
import com.example.demo.entities.SubscriptionPlan;
import com.example.demo.security.annotations.IsAdmin;
import com.example.demo.security.annotations.IsAuthenticated;
import com.example.demo.services.SubscriptionPlanService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/subscription-plans")
@IsAdmin
public class SubscriptionPlanController
        extends BaseController<SubscriptionPlan, UUID, CreatePlanRequest, UpdatePlanRequest, PlanResponse> {

    public SubscriptionPlanController(SubscriptionPlanService service) {
        super(service);
    }

    /**
     * SỬA LỖI: Thay RES bằng PlanResponse (Kiểu thực tế bạn đã khai báo ở trên)
     */
    @Override
    @IsAuthenticated
    public ResponseEntity<Page<PlanResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(service.findAll(pageable));
    }
    /**
     * SỬA LỖI: Thay ID bằng UUID và RES bằng PlanResponse
     */
    @Override
    @IsAuthenticated
    public ResponseEntity<PlanResponse> getById(@PathVariable UUID id) {
        return super.getById(id);
    }
}