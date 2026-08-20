package com.example.demo.modules.feature.controllers;

import com.example.demo.common.base.BaseController;
import com.example.demo.common.security.annotations.IsAdmin;
import com.example.demo.common.security.annotations.IsAuthenticated;
import com.example.demo.modules.feature.dto.CreateFeatureRequest;
import com.example.demo.modules.feature.dto.UpdateFeatureRequest;
import com.example.demo.modules.feature.dto.FeatureResponse;
import com.example.demo.modules.feature.services.IFeatureService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequestMapping("/api/features")
@IsAdmin
public class FeatureController extends BaseController<CreateFeatureRequest, UpdateFeatureRequest, FeatureResponse, UUID> {

    public FeatureController(IFeatureService service) {
        super(service);
    }

    @Override
    @IsAuthenticated
    public ResponseEntity<Page<FeatureResponse>> getAll(Pageable pageable) {
        return super.getAll(pageable);
    }

    @Override
    @IsAuthenticated
    public ResponseEntity<FeatureResponse> getById(@PathVariable UUID id) {
        return super.getById(id);
    }
}
