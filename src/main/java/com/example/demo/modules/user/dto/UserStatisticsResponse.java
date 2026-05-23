package com.example.demo.modules.user.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserStatisticsResponse {
    private int totalUser;
    private int totalActiveUser;
    private int totalBlockUser;
    private List<PlanUserStatisticItem> planStatistics;
}
