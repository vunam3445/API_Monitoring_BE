package com.example.demo.modules.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseTimeChartResponse {
    private String range;
    private String bucket;
    private List<ResponseTimePointResponse> points;
}
