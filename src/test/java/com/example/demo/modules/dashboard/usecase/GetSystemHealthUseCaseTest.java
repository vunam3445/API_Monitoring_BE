package com.example.demo.modules.dashboard.usecase;

import com.example.demo.modules.dashboard.domain.SystemHealthProvider;
import com.example.demo.modules.dashboard.dto.SystemHealthResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class GetSystemHealthUseCaseTest {

    @Test
    void execute_returnsProviderResponse() {
        // Arrange
        SystemHealthProvider provider = Mockito.mock(SystemHealthProvider.class);
        SystemHealthResponse expected = SystemHealthResponse.builder()
                .cpuUsage(12.5)
                .ramUsage(45.0)
                .diskUsage(30.0)
                .pendingQueue(5)
                .isWorkersRunning(true)
                .build();
        Mockito.when(provider.getSystemHealth()).thenReturn(expected);
        GetSystemHealthUseCase useCase = new GetSystemHealthUseCase(provider);

        // Act
        SystemHealthResponse actual = useCase.execute();

        // Assert
        assertEquals(expected, actual);
    }
}
