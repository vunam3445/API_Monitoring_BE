package com.example.demo.modules.revenue.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RevenueChartDTO {
    private List<String> labels;
    private List<DatasetDTO> datasets;

    @Data
    @Builder
    public static class DatasetDTO {
        private String label;
        private List<Double> data;
    }
}
