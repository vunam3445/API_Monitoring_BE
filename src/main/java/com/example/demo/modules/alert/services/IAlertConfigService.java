package com.example.demo.modules.alert.services;

import com.example.demo.modules.alert.dto.AlertConfigResponse;
import com.example.demo.modules.alert.dto.CreateAlertConfigRequest;

import java.util.List;
import java.util.UUID;

public interface IAlertConfigService {
    List<AlertConfigResponse> findAllByMonitorId(UUID monitorId);
    AlertConfigResponse create(UUID monitorId, CreateAlertConfigRequest request);
    AlertConfigResponse update(UUID id, CreateAlertConfigRequest request);
    void toggle(UUID id);
    void delete(UUID id);
}
