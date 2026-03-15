package com.example.demo.controllers;

import com.example.demo.dto.request.SubscriptionPlan.CreatePlanRequest;
import com.example.demo.dto.request.SubscriptionPlan.UpdatePlanRequest;
import com.example.demo.dto.response.SubscriptionPlan.PlanResponse;
import com.example.demo.entities.SubscriptionPlan;
import com.example.demo.services.SubscriptionPlanService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/subscription-plans")
public class SubscriptionPlanController
        extends BaseController<SubscriptionPlan, UUID, CreatePlanRequest, UpdatePlanRequest, PlanResponse> {

    public SubscriptionPlanController(SubscriptionPlanService service) {
        super(service);
    }


}