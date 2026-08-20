package com.example.demo.modules.feature.services;

import com.example.demo.common.cache.ICacheService;
import com.example.demo.common.exceptions.FeatureInUseException;
import com.example.demo.common.exceptions.ResourceNotFoundException;
import com.example.demo.modules.feature.entities.Feature;
import com.example.demo.modules.feature.repositories.FeatureRepository;
import com.example.demo.modules.feature.dto.CreateFeatureRequest;
import com.example.demo.modules.feature.dto.FeatureResponse;
import com.example.demo.modules.feature.mappers.FeatureMapper;
import com.example.demo.modules.subscription.entities.SubscriptionPlan;
import com.example.demo.modules.subscription.repositories.SubscriptionPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FeatureServiceTest {

    @Mock
    private FeatureRepository featureRepository;

    @Mock
    private FeatureMapper featureMapper;

    @Mock
    private ICacheService cacheService;

    @Mock
    private SubscriptionPlanRepository planRepository;

    private FeatureService featureService;

    @BeforeEach
    public void setUp() {
        featureService = new FeatureService(featureRepository, featureMapper, cacheService, planRepository);
    }

    @Test
    public void testCreateFeature_KeyAlreadyExists_ThrowsException() {
        CreateFeatureRequest request = new CreateFeatureRequest();
        request.setKey("test_feature");

        when(featureRepository.existsByKey("test_feature")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> {
            featureService.create(request);
        });

        verify(featureRepository, never()).save(any());
    }

    @Test
    public void testDeleteFeature_FeatureInUse_ThrowsFeatureInUseException() {
        UUID featureId = UUID.randomUUID();
        Feature feature = new Feature();
        feature.setId(featureId);
        feature.setKey("slack_notifications");

        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setName("PRO");
        plan.setFeatures("{\"slack_notifications\": true}");

        when(featureRepository.findById(featureId)).thenReturn(Optional.of(feature));
        when(planRepository.findAll()).thenReturn(Collections.singletonList(plan));

        assertThrows(FeatureInUseException.class, () -> {
            featureService.delete(featureId);
        });

        verify(featureRepository, never()).deleteById(any());
    }

    @Test
    public void testDeleteFeature_FeatureNotUsed_Success() {
        UUID featureId = UUID.randomUUID();
        Feature feature = new Feature();
        feature.setId(featureId);
        feature.setKey("slack_notifications");

        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setName("FREE");
        plan.setFeatures("{\"slack_notifications\": false}");

        when(featureRepository.findById(featureId)).thenReturn(Optional.of(feature));
        when(planRepository.findAll()).thenReturn(Collections.singletonList(plan));
        when(featureRepository.existsById(featureId)).thenReturn(true);

        featureService.delete(featureId);

        verify(featureRepository, times(1)).deleteById(featureId);
    }
}
