package com.example.demo.modules.alert.services;

import com.example.demo.modules.alert.enums.IncidentSeverity;
import com.example.demo.modules.alert.enums.IncidentType;
import com.example.demo.modules.monitor.entities.Monitor;
import com.example.demo.modules.monitor.enums.MonitorEventType;
import com.example.demo.modules.uptimeLogs.entities.UptimeLogs;
import lombok.Builder;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class IncidentRuleEvaluator {

    @Getter
    @Builder
    public static class EvaluationResult {
        private boolean shouldCreateIncident;
        private IncidentType type;
        private IncidentSeverity severity;
        private String title;
        private String message;
    }

    public Optional<EvaluationResult> evaluate(Monitor monitor, UptimeLogs log) {
        // Nếu monitor đang UP thì không có incident (hoặc sẽ dùng để resolve incident hiện có ở service khác)
        if (Boolean.TRUE.equals(log.getIsUp())) {
            // Check for WARNING status if needed (e.g. slow response)
            if ("WARNING".equals(log.getAssertionStatus())) {
                 return Optional.of(evaluateSlowResponse(monitor, log));
            }
            return Optional.empty();
        }

        // Trường hợp isUp = false
        MonitorEventType eventType = log.getEventType();
        if (MonitorEventType.TIMEOUT == eventType) {
            return Optional.of(EvaluationResult.builder()
                    .shouldCreateIncident(true)
                    .type(IncidentType.TIMEOUT)
                    .severity(IncidentSeverity.CRITICAL) 
                    .title("API Timeout: " + monitor.getName())
                    .message("Monitor " + monitor.getName() + " timed out after " + log.getResponseTimeMs() + "ms.")
                    .build());
        }

        if (MonitorEventType.API_FAILURE == eventType) {
             return Optional.of(EvaluationResult.builder()
                    .shouldCreateIncident(true)
                    .type(IncidentType.API_DOWN)
                    .severity(IncidentSeverity.CRITICAL)
                    .title("API Down: " + monitor.getName())
                    .message("Monitor " + monitor.getName() + " is unreachable. Error: " + log.getErrorMessage())
                    .build());
        }

        // Mặc định là STATUS_CODE_ERROR nếu không rơi vào các case trên nhưng isUp = false
        return Optional.of(EvaluationResult.builder()
                .shouldCreateIncident(true)
                .type(IncidentType.STATUS_CODE_ERROR)
                .severity(IncidentSeverity.WARNING)
                .title("Status Code Error: " + monitor.getName())
                .message("Monitor " + monitor.getName() + " returned status code " + log.getStatusCode() + ". Assertion: " + log.getAssertionMessage())
                .build());
    }

    private EvaluationResult evaluateSlowResponse(Monitor monitor, UptimeLogs log) {
        return EvaluationResult.builder()
                .shouldCreateIncident(true)
                .type(IncidentType.SLOW_RESPONSE)
                .severity(IncidentSeverity.WARNING)
                .title("Slow Response: " + monitor.getName())
                .message("Monitor " + monitor.getName() + " response time is " + log.getResponseTimeMs() + "ms (threshold: " + monitor.getMaxResponseTimeMs() + "ms).")
                .build();
    }
}
