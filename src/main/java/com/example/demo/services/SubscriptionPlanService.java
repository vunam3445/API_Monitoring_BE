package com.example.demo.services;

import com.example.demo.dto.request.SubscriptionPlan.CreatePlanRequest;
import com.example.demo.dto.request.SubscriptionPlan.UpdatePlanRequest;
import com.example.demo.dto.response.SubscriptionPlan.PlanResponse;
import com.example.demo.entities.SubscriptionPlan;
import com.example.demo.repositories.subscriptionPlan.SubscriptionPlanRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SubscriptionPlanService extends BaseService<SubscriptionPlan, UUID, CreatePlanRequest, UpdatePlanRequest, PlanResponse> {

    public SubscriptionPlanService(SubscriptionPlanRepository repository) {
        super(repository);
    }

    // Chỉ định các Class để ModelMapper làm việc
    @Override
    protected Class<SubscriptionPlan> getEntityClass() {
        return SubscriptionPlan.class;
    }

    @Override
    protected Class<PlanResponse> getResponseClass() {
        return PlanResponse.class;
    }

}