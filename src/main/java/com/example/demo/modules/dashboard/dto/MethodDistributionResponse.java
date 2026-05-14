package com.example.demo.modules.dashboard.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MethodDistributionResponse {
    private List<MethodCount> distributions;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MethodCount {
        private String method;
        private long count;
        private double percentage;
    }
}
