package com.example.demo.modules.user.services;

import com.example.demo.common.base.RestPageImpl;
import com.example.demo.common.exceptions.ForbidenException;
import com.example.demo.common.exceptions.SubscriptionNotFoundException;
import com.example.demo.common.exceptions.UserNotFoundException;
import com.example.demo.modules.subscription.entities.Subscription;
import com.example.demo.modules.subscription.repositories.SubscriptionPlanRepository;
import com.example.demo.modules.subscription.repositories.SubscriptionRepository;
import com.example.demo.modules.subscription.services.ISubscriptionService;
import com.example.demo.modules.user.dto.UserAdminResponse;
import com.example.demo.modules.user.dto.UserFilterCriteria;
import com.example.demo.modules.user.dto.UserStatisticsResponse;
import com.example.demo.modules.user.entities.User;
import com.example.demo.modules.user.enums.UserStatus;
import com.example.demo.modules.user.enums.UserRole;
import com.example.demo.modules.user.mappers.UserMapper;
import com.example.demo.modules.user.repositories.UserRepository;
import com.example.demo.modules.user.repositories.UserSpecification;
import com.example.demo.modules.monitor.repositories.MonitorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import lombok.extern.slf4j.Slf4j;
import com.example.demo.common.security.ISecurityContextService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.common.cache.ICacheService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import com.example.demo.modules.user.dto.PlanUserStatisticItem;

import com.example.demo.modules.subscription.enums.SubscriptionStatus;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ManagerUserService implements IManagerUserService {
    private final UserRepository userRepository;
    private final UserMapper mapper;
    private final ISecurityContextService securityContextService;
    private final ICacheService cacheService;
    private final ISubscriptionService subscriptionService;
    private final MonitorRepository monitorRepository;


    private static final String CACHE_ADMIN_USERS = "api-monitoring:admin:users:list";
    private static final String CACHE_STATS = "api-monitoring:admin:users:stats";

    @Override
    @Cacheable(value = CACHE_ADMIN_USERS, key = "'filters_' + #userFilterCriteria.toString().hashCode() + " +
            "'_page_' + #pageable.pageNumber + '_size_' + #pageable.pageSize + " +
            "'_sort_' + #pageable.sort.toString()", unless = "#result == null")
    public Page<UserAdminResponse> getAllUser(UserFilterCriteria userFilterCriteria, Pageable pageable) {
        Specification<User> spec = Specification.where(UserSpecification.hasEmail(userFilterCriteria.getEmail())
                .and(UserSpecification.hasFullName(userFilterCriteria.getFullName()))
                .and(UserSpecification.hasRole(userFilterCriteria.getRole()))
                .and(UserSpecification.hasPlanType(userFilterCriteria.getPlanType()))
                .and(UserSpecification.hasStatus(userFilterCriteria.getStatus())));
        Page<User> users = userRepository.findAll(spec, pageable);

        // Tối ưu N+1: Lấy trước toàn bộ monitor count của user trong page
        List<UUID> userIds = users.getContent().stream().map(User::getId).collect(Collectors.toList());
        Map<UUID, Long> monitorCounts = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<Object[]> countsResult = monitorRepository.countMonitorsByUserIds(userIds);
            for (Object[] row : countsResult) {
                monitorCounts.put((UUID) row[0], ((Number) row[1]).longValue());
            }
        }

        // Gán currentPeriodEnd và maxMonitors từ subscription ACTIVE
        List<UserAdminResponse> dtos = users.getContent().stream().map(user -> {
            UserAdminResponse dto = mapper.toUserAdminResponse(user);
            if (user.getSubscriptions() != null) {
                user.getSubscriptions().stream()
                    .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                    .findFirst()
                    .ifPresent(s -> {
                        dto.setCurrentPeriodEnd(s.getCurrentPeriodEnd());
                        dto.setMaxMonitors(s.getMaxMonitors() != null ? s.getMaxMonitors() : 0L);
                    });
            }
            dto.setMonitors(monitorCounts.getOrDefault(user.getId(), 0L));
            return dto;
        }).collect(Collectors.toList());

        return new RestPageImpl<>(
                dtos,
                users.getNumber(),
                users.getSize(),
                users.getTotalElements(),
                null,
                users.isLast(),
                users.getTotalPages(),
                null,
                users.isFirst(),
                users.getNumberOfElements());
    }

    @Override
    public UserAdminResponse blockUser(UUID userId) {
        validateNotSelf(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy user"));
        user.setStatus(UserStatus.SUSPENDED);
        userRepository.saveAndFlush(user);

        // Xóa cache danh sách admin và danh sách người dùng chung
        evictUserCaches(userId);

        return mapper.toUserAdminResponse(user);
    }

    @Override
    public UserAdminResponse activeUser(UUID userId) {
        validateNotSelf(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy user"));
        user.setStatus(UserStatus.ACTIVE);
        userRepository.saveAndFlush(user);

        // Xóa cache danh sách admin và danh sách người dùng chung
        evictUserCaches(userId);

        return mapper.toUserAdminResponse(user);
    }

    private void evictUserCaches(UUID userId) {
        // 1. Xóa cache danh sách admin (đa tiêu chí lọc)
        cacheService.evictByPrefix(CACHE_ADMIN_USERS + "::");

        // 2. Xóa cache danh sách người dùng chung (từ BaseService của UserService)
        cacheService.evictByPrefix("api-monitoring:api:list::user");

        // 3. Xóa cache object cá nhân của user (nếu có)
        if (userId != null) {
            cacheService.evict("api-monitoring:api:object::user:" + userId);
        }
        
        // 4. Xóa cache thống kê
        cacheService.evictByPrefix(CACHE_STATS);
    }

    private void validateNotSelf(UUID targetUserId) {
        securityContextService.getCurrentUserId().ifPresent(currentUserId -> {
            if (currentUserId.equals(targetUserId)) {
                throw new ForbidenException("Bạn không thể thực hiện thao tác này trên chính tài khoản của mình");
            }
        });
    }

    @Override
    @Cacheable(value = CACHE_STATS, key = "'all'", unless = "#result == null")
    public UserStatisticsResponse countUserAndPlanUser() {
        UserStatisticsResponse response = new UserStatisticsResponse();

        // Query 1: Thống kê theo trạng thái (Loại trừ ADMIN và Plan rỗng)
        List<Object[]> statusCounts = userRepository.countUsersByStatus(UserRole.ADMIN);
        int total = 0;
        int active = 0;
        int blocked = 0;

        for (Object[] row : statusCounts) {
            UserStatus status = (UserStatus) row[0];
            int count = ((Long) row[1]).intValue();
            total += count;
            if (status == UserStatus.ACTIVE) active = count;
            if (status == UserStatus.SUSPENDED) blocked = count;
        }

        response.setTotalUser(total);
        response.setTotalActiveUser(active);
        response.setTotalBlockUser(blocked);

        // Query 2: Thống kê theo Plan (Loại trừ ADMIN và Plan rỗng)
        List<Object[]> planCounts = userRepository.countUsersByPlan(UserRole.ADMIN);
        List<PlanUserStatisticItem> planStatistics = planCounts.stream()
                .map(row -> {
                    PlanUserStatisticItem item = new PlanUserStatisticItem();
                    item.setPlanName((String) row[0]);
                    item.setTotalUser(((Long) row[1]).intValue());
                    return item;
                })
                .collect(Collectors.toList());

        response.setPlanStatistics(planStatistics);
        return response;
    }

    @Override
    public void updatePlanForUser(UUID userId, UUID planId) {
        // 1. Cập nhật Subscription và User dưới database (Logic uỷ quyền cho SubscriptionService)
        subscriptionService.updatePlanByAdmin(userId, planId);

        // 2. Xoá cache phía admin để danh sách User/Stats được cập nhật ngay lập tức
        evictUserCaches(userId);
    }
}
