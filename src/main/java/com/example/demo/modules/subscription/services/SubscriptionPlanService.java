package com.example.demo.modules.subscription.services;

import com.example.demo.common.base.BaseService;

import com.example.demo.common.cache.ICacheService;
import com.example.demo.modules.subscription.dto.CreatePlanRequest;
import com.example.demo.modules.subscription.dto.UpdatePlanRequest;
import com.example.demo.modules.subscription.dto.PlanResponse;
import com.example.demo.modules.subscription.entities.SubscriptionPlan;
import com.example.demo.modules.subscription.mappers.SubscriptionPlanMapper;
import com.example.demo.modules.subscription.repositories.SubscriptionPlanRepository;
import com.example.demo.modules.subscription.services.ISubscriptionPlanService;
import com.example.demo.modules.user.repositories.UserRepository;
import com.example.demo.modules.user.entities.User;
import com.example.demo.common.security.ISecurityContextService;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.demo.common.base.RestPageImpl;
import java.util.List;
import java.util.stream.Collectors;

import java.util.UUID;

@Service
public class SubscriptionPlanService
        extends BaseService<SubscriptionPlan, UUID, CreatePlanRequest, UpdatePlanRequest, PlanResponse>
        implements ISubscriptionPlanService {

    private final UserRepository userRepository;
    private final ISecurityContextService securityContextService;


    public SubscriptionPlanService(SubscriptionPlanRepository repository,
                                   SubscriptionPlanMapper mapper,
                                   ICacheService cacheService,
                                   UserRepository userRepository,
                                   ISecurityContextService securityContextService) {
        super(repository, mapper, cacheService);
        this.userRepository = userRepository;
        this.securityContextService = securityContextService;
    }


    @Override
    protected Class<SubscriptionPlan> getEntityClass() {
        return SubscriptionPlan.class;
    }

    @Override
    protected void evictListCache() {
        // 1. Xóa cache mặc định của BaseService
        super.evictListCache();
        // 2. Xóa các cache đặc thù của SubscriptionPlan
        cacheService.evictByPrefix("api-monitoring:subscription-plans");
        cacheService.evictByPrefix("api-monitoring:subscription-plans-all");
    }

    @Override
    @Cacheable(value = "api-monitoring:subscription-plans", key = "'user_status_' + #userId.toString()", unless = "#result == null")
    public List<PlanResponse> findAllWithUserStatus(UUID userId) {
        // 1. Lấy user để biết đang dùng gói nào
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.example.demo.common.exceptions.ResourceNotFoundException("User not found: " + userId));

        UUID currentPlanId = user.getSubscriptionPlan() != null ? user.getSubscriptionPlan().getId() : null;

        // 2. Lấy toàn bộ danh sách gói đang hoạt động
        List<SubscriptionPlan> plans = repository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .collect(Collectors.toList());

        // 3. Map sang DTO và gán cờ isCurrentPlan
        return plans.stream().map(plan -> {
            PlanResponse res = mapper.toResponse(plan);
            res.setIsCurrentPlan(plan.getId().equals(currentPlanId));
            return res;
        }).collect(Collectors.toList());
    }
    @Override
    @Cacheable(value = "api-monitoring:subscription-plans-paged", 
               key = "'user_plan_' + (T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication() != null && T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getPrincipal() instanceof T(com.example.demo.modules.user.entities.User) ? (T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getPrincipal().getSubscriptionPlan() != null ? T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getPrincipal().getSubscriptionPlan().getId() : 'none') : 'guest') + '_page_' + #pageable.pageNumber + '_size_' + #pageable.pageSize + '_sort_' + #pageable.sort.toString().replaceAll(':', '_')", 
               unless = "#result == null")
    public Page<PlanResponse> findAll(Pageable pageable) {
        // 1. Lấy thông tin gói hiện tại của user từ SecurityContext
        UUID currentPlanId = getCurrentUserPlanId();

        // 2. Lấy dữ liệu phân trang từ DB
        Page<SubscriptionPlan> entityPage = repository.findAll(pageable);

        // 3. Map sang DTO và gán isCurrentPlan
        List<PlanResponse> dtos = entityPage.getContent().stream().map(plan -> {
            PlanResponse res = mapper.toResponse(plan);
            if (currentPlanId != null) {
                res.setIsCurrentPlan(plan.getId().equals(currentPlanId));
            } else {
                res.setIsCurrentPlan(false);
            }
            return res;
        }).collect(Collectors.toList());

        return new RestPageImpl<>(
                dtos,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                null, false, 0, null, false, 0
        );
    }

    @Override
    @Cacheable(value = "api-monitoring:subscription-plans-all", unless = "#result == null")
    public List<PlanResponse> findAllPlans() {
        UUID currentPlanId = getCurrentUserPlanId();
        return repository.findAll().stream().map(plan -> {
            PlanResponse res = mapper.toResponse(plan);
            if (currentPlanId != null) {
                res.setIsCurrentPlan(plan.getId().equals(currentPlanId));
            } else {
                res.setIsCurrentPlan(false);
            }
            return res;
        }).collect(Collectors.toList());
    }


    private UUID getCurrentUserPlanId() {
        return securityContextService.getCurrentUser()
                .map(user -> user.getSubscriptionPlan() != null ? user.getSubscriptionPlan().getId() : null)
                .orElse(null);
    }

}
