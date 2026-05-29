package com.example.demo.modules.feature.services;

import com.example.demo.common.base.BaseService;
import com.example.demo.common.cache.ICacheService;
import com.example.demo.common.exceptions.FeatureInUseException;
import com.example.demo.common.exceptions.ResourceNotFoundException;
import com.example.demo.modules.feature.entities.Feature;
import com.example.demo.modules.feature.repositories.FeatureRepository;
import com.example.demo.modules.feature.dto.CreateFeatureRequest;
import com.example.demo.modules.feature.dto.UpdateFeatureRequest;
import com.example.demo.modules.feature.dto.FeatureResponse;
import com.example.demo.modules.feature.mappers.FeatureMapper;
import com.example.demo.modules.subscription.entities.SubscriptionPlan;
import com.example.demo.modules.subscription.repositories.SubscriptionPlanRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FeatureService 
        extends BaseService<Feature, UUID, CreateFeatureRequest, UpdateFeatureRequest, FeatureResponse>
        implements IFeatureService {

    private final SubscriptionPlanRepository planRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FeatureService(FeatureRepository repository,
                          FeatureMapper mapper,
                          ICacheService cacheService,
                          SubscriptionPlanRepository planRepository) {
        super(repository, mapper, cacheService);
        this.planRepository = planRepository;
    }

    @Override
    protected Class<Feature> getEntityClass() {
        return Feature.class;
    }

    @Override
    @Transactional
    public FeatureResponse create(CreateFeatureRequest requestDto) {
        if (((FeatureRepository) repository).existsByKey(requestDto.getKey())) {
            throw new IllegalArgumentException("Feature với key '" + requestDto.getKey() + "' đã tồn tại.");
        }
        return super.create(requestDto);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Feature feature = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tính năng để xóa. ID: " + id));

        String key = feature.getKey();
        List<SubscriptionPlan> activePlans = planRepository.findAll();

        for (SubscriptionPlan plan : activePlans) {
            if (plan.getFeatures() != null && !plan.getFeatures().trim().isEmpty()) {
                try {
                    Map<String, Object> featureMap = objectMapper.readValue(plan.getFeatures(), new TypeReference<Map<String, Object>>() {});
                    if (featureMap.containsKey(key) && Boolean.TRUE.equals(featureMap.get(key))) {
                        throw new FeatureInUseException("Không thể xóa tính năng này vì nó đang hoạt động trong gói cước: " + plan.getName());
                    }
                } catch (Exception e) {
                    // Log error or ignore
                }
            }
        }

        super.delete(id);
    }
}
