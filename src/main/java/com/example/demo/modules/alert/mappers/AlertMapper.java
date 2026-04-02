package com.example.demo.modules.alert.mappers;

import com.example.demo.modules.alert.dto.AlertConfigResponse;
import com.example.demo.modules.alert.dto.AlertListItemResponse;
import com.example.demo.modules.alert.entities.AlertConfig;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AlertMapper {
    public AlertConfigResponse toConfigResponse(AlertConfig entity) {
        if (entity == null) return null;
        return AlertConfigResponse.builder()
                .id(entity.getId())
                .monitorId(entity.getMonitor().getId())
                .type(entity.getType())
                .destination(entity.getDestination())
                .isEnabled(entity.getIsEnabled())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public List<AlertConfigResponse> toConfigResponseList(List<AlertConfig> entities) {
        return entities.stream().map(this::toConfigResponse).collect(Collectors.toList());
    }

    public AlertListItemResponse toListItemResponse(com.example.demo.modules.alert.entities.Incident incident) {
        if (incident == null) return null;
        return AlertListItemResponse.builder()
                .id(incident.getId())
                .time(incident.getTriggeredAt())
                .apiName(incident.getMonitor().getName())
                .endpoint(incident.getMonitor().getUrl())
                .alertType(incident.getType())
                .severity(incident.getSeverity())
                .status(incident.getStatus())
                .message(incident.getMessage())
                .build();
    }

    public com.example.demo.modules.alert.dto.AlertDetailResponse toDetailResponse(com.example.demo.modules.alert.entities.Incident incident, List<com.example.demo.modules.alert.entities.AlertDelivery> deliveries) {
        if (incident == null) return null;
        
        List<com.example.demo.modules.alert.dto.AlertDetailResponse.DeliveryLogResponse> history = deliveries == null ? null : 
            deliveries.stream().map(d -> com.example.demo.modules.alert.dto.AlertDetailResponse.DeliveryLogResponse.builder()
                .id(d.getId())
                .channel(d.getChannel().toString())
                .destination(d.getDestination())
                .status(d.getStatus().toString())
                .sentAt(d.getSentAt())
                .errorMessage(d.getErrorMessage())
                .build()
            ).collect(Collectors.toList());

        return com.example.demo.modules.alert.dto.AlertDetailResponse.builder()
                .id(incident.getId())
                .title(incident.getTitle())
                .message(incident.getMessage())
                .alertType(incident.getType())
                .severity(incident.getSeverity())
                .status(incident.getStatus())
                .monitorId(incident.getMonitor().getId())
                .monitorName(incident.getMonitor().getName())
                .monitorUrl(incident.getMonitor().getUrl())
                .triggeredAt(incident.getTriggeredAt())
                .lastSeenAt(incident.getLastSeenAt())
                .resolvedAt(incident.getResolvedAt())
                .consecutiveFailCount(incident.getConsecutiveFailCount())
                .avgLatencyMs(incident.getAvgLatencyMs())
                .lastStatusCode(incident.getLastStatusCode())
                .deliveryHistory(history)
                .build();
    }
}
