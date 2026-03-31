package com.example.demo.modules.monitor.services;

import com.example.demo.common.base.BaseMapper;
import com.example.demo.common.base.BaseService;
import com.example.demo.common.cache.ICacheService;
import com.example.demo.common.exceptions.ResourceNotFoundException;
import com.example.demo.common.security.ISecurityContextService;
import com.example.demo.modules.monitor.dto.ApiResponse;
import com.example.demo.modules.monitor.dto.CreateApiRequest;
import com.example.demo.modules.monitor.dto.UpdateApiRequest;
import com.example.demo.common.exceptions.AuthenticationException;
import com.example.demo.common.exceptions.ForbidenException;
import com.example.demo.modules.user.entities.User;

import com.example.demo.modules.monitor.entities.Monitor;
import com.example.demo.modules.monitor.mappers.MonitorMapper;
import com.example.demo.modules.monitor.repositories.MonitorRepository;
import com.example.demo.modules.monitor.lock.DistributedLockService;
import com.example.demo.modules.monitor.messaging.MonitorProducer;
import com.example.demo.modules.subscription.entities.SubscriptionPlan;
import com.example.demo.modules.user.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.example.demo.modules.monitor.repositories.MonitorSpecification;
import org.springframework.data.jpa.domain.Specification;
import com.example.demo.common.base.RestPageImpl;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MonitorService
        extends BaseService<Monitor, UUID, CreateApiRequest, UpdateApiRequest, ApiResponse>
        implements IMonitorService
{
    private final MonitorRepository monitorRepository;
    private final DistributedLockService lockService;
    private final MonitorProducer monitorProducer;
    private final ISecurityContextService iSecurityContextService;
    private final UserRepository userRepository;

    public MonitorService(
            MonitorRepository repository,
            MonitorMapper mapper,
            ICacheService cacheService,
            DistributedLockService lockService,
            ISecurityContextService iSecurityContextService,
            UserRepository userRepository,
            MonitorProducer monitorProducer) {
        super(repository, mapper, cacheService);
        this.monitorRepository = repository;
        this.lockService = lockService;
        this.monitorProducer = monitorProducer;
        this.iSecurityContextService = iSecurityContextService;
        this.userRepository = userRepository;
    }

    @Override
    protected Class<Monitor> getEntityClass() {
        return Monitor.class;
    }

    @Override
    @Cacheable(value = "api-monitoring:api:list",
            key = "'user_' + #userId.toString() + '_status_' + (#lastStatus ?: 'all') + '_active_' + (#isActive ?: 'all') + '_search_' + (#search ?: 'none') + '_page_' + #pageable.pageNumber + '_size_' + #pageable.pageSize + '_sort_' + #pageable.sort.toString().replaceAll(':', '_')",
            unless = "#result == null")
    public Page<ApiResponse> findAllByUserId(UUID userId, String lastStatus, Boolean isActive, String search, Pageable pageable) {
        Specification<Monitor> spec = Specification.where(MonitorSpecification.hasUserId(userId))
                .and(MonitorSpecification.hasStatus(lastStatus))
                .and(MonitorSpecification.hasActive(isActive))
                .and(MonitorSpecification.hasNameLike(search));

        Page<Monitor> entityPage = monitorRepository.findAll(spec, pageable);
        List<ApiResponse> dtos = mapper.toResponseList(entityPage.getContent());

        return new RestPageImpl<>(
                dtos,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                null, false, 0, null, false, 0
        );
    }

    @Override
    @Transactional
    public Boolean updateAPIStatus(UUID id) {
        Monitor monitor = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dữ liệu: " + id));
        
        monitor.setIsActive(!monitor.getIsActive());
        
        evictObjectCache(monitor.getId());
        evictListCache();
        
        return repository.save(monitor).getIsActive();
    }
    @Override
    public Boolean retry(UUID id) {
        Monitor monitor = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dữ liệu: " + id));

        String monitorId = monitor.getId().toString();

        // Thử claim lock với TTL 120s (giống Scheduler)
        if (lockService.tryLock(monitorId, 120)) {
            try {
                monitorProducer.sendExecutionJob(monitorId);
                return true;
            } catch (Exception e) {
                lockService.unlock(monitorId);
                throw new RuntimeException("Gửi yêu cầu retry thất bại: " + e.getMessage());
            }
        }

        return false; // Đã có lock (đang được chạy bởi worker khác hoặc scheduler)
    }

    @Override
    @Transactional
    public ApiResponse create(CreateApiRequest request) {
        User currentUser = iSecurityContextService.getCurrentUser()
                .orElseThrow(() -> new AuthenticationException("Bạn cần đăng nhập để thực hiện thao tác này."));

        // Re-fetch user from database to ensure it's managed by the current session
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new AuthenticationException("Không tìm thấy thông tin người dùng."));

        SubscriptionPlan plan = user.getSubscriptionPlan();
        if (plan == null) {
            throw new ForbidenException("Người dùng hiện tại chưa có gói đăng ký hợp lệ.");
        }

        // 1. Kiểm tra số lượng monitor tối đa của gói
        long currentMonitors = monitorRepository.countByUserId(user.getId());
        if (currentMonitors >= plan.getMaxMonitors()) {
            throw new ForbidenException("Bạn đã đạt tới giới hạn tối đa (" + plan.getMaxMonitors() + ") monitor của gói " + plan.getName() + ".");
        }

        // 2. Kiểm tra khoảng thời gian check tối thiểu của gói
        if (request.getCheckInterval() < plan.getMinInterval()) {
            throw new ForbidenException("Gói " + plan.getName() + " chỉ hỗ trợ khoảng thời gian kiểm tra tối thiểu là " + plan.getMinInterval() + " giây.");
        }

        // 3. Tạo mới monitor
        Monitor monitor = mapper.toEntity(request);
        monitor.setUserId(user.getId());
        
        Monitor savedMonitor = repository.saveAndFlush(monitor);

        // Xóa cache danh sách
        evictListCache();

        return mapper.toResponse(savedMonitor);
    }
}
