package com.example.demo.modules.feature.mappers;

import com.example.demo.common.base.BaseMapper;
import com.example.demo.modules.feature.entities.Feature;
import com.example.demo.modules.feature.dto.CreateFeatureRequest;
import com.example.demo.modules.feature.dto.UpdateFeatureRequest;
import com.example.demo.modules.feature.dto.FeatureResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface FeatureMapper extends BaseMapper<Feature, CreateFeatureRequest, UpdateFeatureRequest, FeatureResponse> {
    @Override
    Feature toEntity(CreateFeatureRequest createRequest);

    @Override
    FeatureResponse toResponse(Feature entity);

    @Override
    void updateEntityFromDto(UpdateFeatureRequest updateRequest, @MappingTarget Feature entity);
}
