package com.example.demo.modules.subscription.services;

import com.example.demo.common.base.BaseService;

import com.example.demo.modules.subscription.dto.CreatePlanRequest;
import com.example.demo.modules.subscription.dto.UpdatePlanRequest;
import com.example.demo.modules.subscription.dto.PlanResponse;
import com.example.demo.modules.subscription.entities.SubscriptionPlan;
import com.example.demo.modules.subscription.mappers.SubscriptionPlanMapper;
import com.example.demo.modules.subscription.repositories.SubscriptionPlanRepository;
import com.example.demo.common.cache.ICacheService;
import com.example.demo.modules.subscription.services.ISubscriptionPlanService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SubscriptionPlanService
        extends BaseService<SubscriptionPlan, UUID, CreatePlanRequest, UpdatePlanRequest, PlanResponse>
        implements ISubscriptionPlanService {

    public SubscriptionPlanService(SubscriptionPlanRepository repository,
                                   SubscriptionPlanMapper mapper,
                                   ICacheService cacheService) {
        super(repository, mapper, cacheService);
    }

    @Override
    protected Class<SubscriptionPlan> getEntityClass() {
        return SubscriptionPlan.class;
    }
}
