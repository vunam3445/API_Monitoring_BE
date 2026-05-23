package com.example.demo.modules.revenue.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PlanBreakdownDTO {
    private String id;
    private String name;
    private Long activeSubscribers;
    private BigDecimal monthlyRevenue;
    private Long churned30d;
    private Double retention;
    private Double growth;
}
