package com.example.demo.modules.subscription.mappers;

import com.example.demo.common.base.BaseMapper;
import com.example.demo.modules.subscription.dto.CreatePlanRequest;
import com.example.demo.modules.subscription.dto.UpdatePlanRequest;
import com.example.demo.modules.subscription.dto.PlanResponse;
import com.example.demo.modules.subscription.entities.SubscriptionPlan;
import org.mapstruct.*;

/**
 * MapStruct mapper cho SubscriptionPlan.
 * Spring sẽ tự quản lý bean này nhờ componentModel = "spring".
 * nullValuePropertyMappingStrategy = IGNORE: field null trong DTO sẽ không ghi đè entity.
 */
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface SubscriptionPlanMapper
        extends BaseMapper<SubscriptionPlan, CreatePlanRequest, UpdatePlanRequest, PlanResponse> {

    @Override
    SubscriptionPlan toEntity(CreatePlanRequest createRequest);

    @Override
    PlanResponse toResponse(SubscriptionPlan entity);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(UpdatePlanRequest updateRequest, @MappingTarget SubscriptionPlan entity);
}
