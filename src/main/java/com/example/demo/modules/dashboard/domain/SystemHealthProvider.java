package com.example.demo.modules.dashboard.domain;

import com.example.demo.modules.dashboard.dto.SystemHealthResponse;

public interface SystemHealthProvider {
    SystemHealthResponse getSystemHealth();
}
