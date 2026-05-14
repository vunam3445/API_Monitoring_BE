package com.example.demo.modules.dashboard.usecase;

import com.example.demo.modules.dashboard.domain.SystemHealthProvider;
import com.example.demo.modules.dashboard.dto.SystemHealthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetSystemHealthUseCase {

    private final SystemHealthProvider systemHealthProvider;

    public SystemHealthResponse execute() {
        return systemHealthProvider.getSystemHealth();
    }
}
