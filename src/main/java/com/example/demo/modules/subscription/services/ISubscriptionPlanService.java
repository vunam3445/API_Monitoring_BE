package com.example.demo.modules.subscription.services;

import com.example.demo.common.base.ICrudService;
import com.example.demo.modules.subscription.dto.CreatePlanRequest;
import com.example.demo.modules.subscription.dto.UpdatePlanRequest;
import com.example.demo.modules.subscription.dto.PlanResponse;

import java.util.UUID;

/**
 * Interface đặc thù cho SubscriptionPlan.
 * Controller sẽ phụ thuộc vào interface này thay vì class SubscriptionPlanService cụ thể.
 * Tuân thủ nguyên lý DIP: phụ thuộc vào abstraction, không phụ thuộc vào implementation.
 */
public interface ISubscriptionPlanService
        extends ICrudService<CreatePlanRequest, UpdatePlanRequest, PlanResponse, UUID> {

    // Có thể thêm các phương thức đặc thù cho SubscriptionPlan tại đây
    // Ví dụ: PlanResponse findByName(String name);
}
