package com.example.demo.modules.alert.services;

import com.example.demo.modules.alert.dto.AlertListItemResponse;
import com.example.demo.modules.alert.dto.AlertListResponse;
import com.example.demo.modules.alert.entities.Incident;
import com.example.demo.modules.alert.enums.IncidentSeverity;
import com.example.demo.modules.alert.enums.IncidentStatus;
import com.example.demo.modules.alert.enums.IncidentType;
import com.example.demo.modules.alert.mappers.AlertMapper;
import com.example.demo.modules.alert.repositories.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlertQueryService {

    public byte[] exportCsv(UUID userId, IncidentStatus status, IncidentSeverity severity, IncidentType type, LocalDateTime from, LocalDateTime to) {
        Specification<Incident> spec = Specification.where((root, query, cb) -> cb.equal(root.get("monitor").get("userId"), userId));
        
        if (status != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        if (severity != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("severity"), severity));
        if (type != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), type));
        if (from != null) spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("triggeredAt"), from));
        if (to != null) spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("triggeredAt"), to));

        List<Incident> list = incidentRepository.findAll(spec);
        
        StringBuilder csv = new StringBuilder("ID,Time,Monitor,URL,Type,Severity,Status,Message\r\n");
        for (Incident i : list) {
            csv.append(i.getId()).append(",");
            csv.append(i.getTriggeredAt()).append(",");
            csv.append(escapeCsv(i.getMonitor().getName())).append(",");
            csv.append(escapeCsv(i.getMonitor().getUrl())).append(",");
            csv.append(i.getType()).append(",");
            csv.append(i.getSeverity()).append(",");
            csv.append(i.getStatus()).append(",");
            csv.append(escapeCsv(i.getMessage())).append("\r\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private final IncidentRepository incidentRepository;
    private final com.example.demo.modules.alert.repositories.AlertDeliveryRepository alertDeliveryRepository;
    private final AlertMapper mapper;

    @org.springframework.cache.annotation.Cacheable(value = "alerts:detail", key = "#id")
    public com.example.demo.modules.alert.dto.AlertDetailResponse findById(UUID userId, UUID id) {
        com.example.demo.modules.alert.entities.Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new com.example.demo.common.exceptions.ResourceNotFoundException("Incident không tồn tại: " + id));
        
        if (!incident.getMonitor().getUserId().equals(userId)) {
             throw new com.example.demo.common.exceptions.ForbidenException("Bạn không có quyền truy cập dữ liệu này.");
        }
        
        List<com.example.demo.modules.alert.entities.AlertDelivery> deliveries = alertDeliveryRepository.findAllByIncidentIdOrderByCreatedAtDesc(id);
        
        return mapper.toDetailResponse(incident, deliveries);
    }

    @org.springframework.cache.annotation.Cacheable(value = "alerts:list", 
        key = "#userId + ':' + (#search ?: '') + ':' + (#status ?: 'ALL') + ':' + (#severity ?: 'ALL') + ':' + (#type ?: 'ALL') + ':' + (#from ?: '0') + ':' + (#to ?: '0') + ':' + #pageable.pageNumber")
    public AlertListResponse findAll(
            UUID userId,
            String search,
            IncidentStatus status,
            IncidentSeverity severity,
            IncidentType type,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable) {
        
        Specification<Incident> spec = Specification.where((root, query, cb) -> cb.equal(root.get("monitor").get("userId"), userId));
        
        if (search != null && !search.isBlank()) {
            spec = spec.and((root, query, cb) -> {
                String keyword = "%" + search.toLowerCase() + "%";
                return cb.or(
                    cb.like(cb.lower(root.get("title")), keyword),
                    cb.like(cb.lower(root.get("message")), keyword),
                    cb.like(cb.lower(root.get("monitor").get("name")), keyword),
                    cb.like(cb.lower(root.get("monitor").get("url")), keyword)
                );
            });
        }
        
        if (status != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        if (severity != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("severity"), severity));
        if (type != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), type));
        if (from != null) spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("triggeredAt"), from));
        if (to != null) spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("triggeredAt"), to));

        Page<Incident> page = incidentRepository.findAll(spec, pageable);
        
        List<AlertListItemResponse> items = page.getContent().stream()
                .map(mapper::toListItemResponse)
                .collect(Collectors.toList());
                
        return AlertListResponse.builder()
                .items(items)
                .page(page.getNumber())
                .size(page.getSize())
                .totalItems(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
