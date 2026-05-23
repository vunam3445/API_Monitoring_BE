package com.example.demo.modules.revenue.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RevenueStatsDTO {
    private BigDecimal totalRevenue;
    private Double revenueGrowth;
    private BigDecimal mrr;
    private Double mrrGrowth;
    private Long activeSubscriptions;
    private Double subsGrowth;
    private Long expiringSoon;
    private Double arpu;
    private Double ltv;
}
