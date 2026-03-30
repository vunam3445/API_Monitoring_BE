package com.example.demo.modules.monitor.services;

import com.example.demo.common.base.ICrudService;
import com.example.demo.modules.monitor.dto.ApiResponse;
import com.example.demo.modules.monitor.dto.CreateApiRequest;
import com.example.demo.modules.monitor.dto.UpdateApiRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface IMonitorService
        extends ICrudService<CreateApiRequest, UpdateApiRequest, ApiResponse, UUID> {
    Page<ApiResponse> findAllByUserId(UUID userId, String lastStatus, Boolean isActive, String search, Pageable pageable);

    Boolean updateAPIStatus(UUID id);

    Boolean retry(UUID id);
}
