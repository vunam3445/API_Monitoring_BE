package com.example.demo.modules.alert.services;

import com.example.demo.modules.alert.dto.AdminUserMonitorAlertResponse;
import com.example.demo.modules.alert.dto.AlertSummaryResponse;

import java.util.UUID;

public interface IAlertSummaryService {
    AlertSummaryResponse getSummary(UUID userId, String range);

    AdminUserMonitorAlertResponse countAlertsAndIncidentsInMonth(UUID userId);

}
