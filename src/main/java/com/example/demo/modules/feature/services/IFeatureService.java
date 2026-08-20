package com.example.demo.modules.feature.services;

import com.example.demo.common.base.ICrudService;
import com.example.demo.modules.feature.dto.CreateFeatureRequest;
import com.example.demo.modules.feature.dto.UpdateFeatureRequest;
import com.example.demo.modules.feature.dto.FeatureResponse;
import java.util.UUID;

public interface IFeatureService extends ICrudService<CreateFeatureRequest, UpdateFeatureRequest, FeatureResponse, UUID> {
}
