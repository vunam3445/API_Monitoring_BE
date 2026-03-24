package com.example.demo.modules.user.services;

import com.example.demo.common.base.BaseService;
import com.example.demo.common.cache.ICacheService;
import com.example.demo.common.exceptions.ForbidenException;
import com.example.demo.common.exceptions.ResourceNotFoundException;
import com.example.demo.modules.subscription.entities.SubscriptionPlan;
import com.example.demo.modules.user.dto.CreateUserSettingRequest;
import com.example.demo.modules.user.dto.UpdateUserSettingRequest;
import com.example.demo.modules.user.dto.UserSettingResponse;
import com.example.demo.modules.user.entities.User;
import com.example.demo.modules.user.entities.UserSetting;
import com.example.demo.modules.user.mappers.UserSettingMapper;
import com.example.demo.modules.user.repositories.UserRepository;
import com.example.demo.modules.user.repositories.UserSettingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class UserSettingService extends BaseService<UserSetting, UUID, CreateUserSettingRequest, UpdateUserSettingRequest, UserSettingResponse>
        implements IUserSettingService {

    private final UserSettingRepository userSettingRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserSettingService(UserSettingRepository repository, UserSettingMapper mapper, ICacheService cacheService) {
        super(repository, mapper, cacheService);
        this.userSettingRepository = repository;
    }

    @Override
    protected Class<UserSetting> getEntityClass() {
        return UserSetting.class;
    }

    @Override
    public void delete(UUID id) {
        throw new ForbidenException("Bạn không có quyền xóa setting!");
    }

    @Override
    public UserSettingResponse create(CreateUserSettingRequest request) {
        throw new ForbidenException("Bạn không có quyền tạo mới setting!");
    }

    @Override
    public UserSettingResponse update(UUID id, UpdateUserSettingRequest request) {
        // 1. Lấy dữ liệu kèm Plan (Dùng join fetch trong repo để tránh N+1)
        UserSetting userSetting = userSettingRepository.findByIdWithUserAndPlan(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cài đặt!"));

        SubscriptionPlan plan = userSetting.getUser().getSubscriptionPlan();
        if (plan == null) {
            throw new ForbidenException("Người dùng chưa được gán gói dịch vụ!");
        }

        // 2. Thực hiện kiểm tra các ràng buộc của Plan
        validateSubscriptionConstraints(request, plan);

        // 3. Sử dụng Mapper để map dữ liệu từ DTO vào Entity
        // MapStruct sẽ tự bỏ qua các field null nhờ cấu hình IGNORE của bạn
        mapper.updateEntityFromDto(request, userSetting);

        // 4. Lưu xuống DataBase
        UserSetting savedEntity = userSettingRepository.save(userSetting);

        // 5. Xoá cache để dữ liệu giao diện nhận được là mới nhất
        evictObjectCache(id);
        evictListCache();

        return mapper.toResponse(savedEntity);
    }

    /**
     * Hàm chuyên biệt để kiểm tra các giới hạn của Gói dịch vụ
     */
    private void validateSubscriptionConstraints(UpdateUserSettingRequest request, SubscriptionPlan plan) {
        // Lấy danh sách tính năng từ dạng chuỗi JSON
        Map<String, Object> features = new HashMap<>();
        if (plan.getFeatures() != null && !plan.getFeatures().trim().isEmpty()) {
            try {
                features = objectMapper.readValue(plan.getFeatures(), new TypeReference<Map<String, Object>>() {
                });
            } catch (JsonProcessingException e) {
                log.error("Lỗi parse cấu hình features của gói dịch vụ: {}", plan.getName(), e);
            }
        }

        // Kiểm tra Tần suất (Interval)
        if (request.getCheckInterval() != null && request.getCheckInterval() < plan.getMinInterval()) {
            throw new ForbidenException(
                    String.format("Gói %s yêu cầu tần suất tối thiểu là %ss. Bạn đang cố gắng set %ss.",
                            plan.getName(), plan.getMinInterval(), request.getCheckInterval())
            );
        }

        // Kiểm tra tính năng Email Alerts
        if (Boolean.TRUE.equals(request.getEmailAlertsEnabled())) {
            boolean isEmailSupported = Boolean.TRUE.equals(features.get("email_notifications"));
            if (!isEmailSupported) {
                throw new ForbidenException("Gói " + plan.getName() + " không hỗ trợ tính năng thông báo qua Email.");
            }
        }

        // Kiểm tra tính năng Slack Webhook
        if (Boolean.TRUE.equals(request.getSlackEnabled())) {
            boolean isSlackSupported = Boolean.TRUE.equals(features.get("slack_notifications"));
            if (!isSlackSupported) {
                throw new ForbidenException("Gói " + plan.getName() + " không hỗ trợ tính năng thông báo qua Slack.");
            }
        }
    }

}
