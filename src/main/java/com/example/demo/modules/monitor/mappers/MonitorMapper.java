package com.example.demo.modules.monitor.mappers;

import com.example.demo.common.base.BaseMapper;
import com.example.demo.modules.monitor.dto.AdminUserMonitorStatsDto;
import com.example.demo.modules.monitor.dto.ApiResponse;
import com.example.demo.modules.monitor.dto.CreateApiRequest;
import com.example.demo.modules.monitor.dto.UpdateApiRequest;
import com.example.demo.modules.monitor.entities.Monitor;
import org.mapstruct.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MonitorMapper extends BaseMapper<Monitor, CreateApiRequest, UpdateApiRequest, ApiResponse> {

    @Override
    // Chạy từ DTO (List) -> Entity (Map)
    @Mapping(target = "headers", source = "headers", qualifiedByName = "mapHeadersListToMap")
    @Mapping(target = "queryParams", source = "queryParams", qualifiedByName = "mapHeadersListToMap")
    Monitor toEntity(CreateApiRequest createRequest);

    @Override
    // Chạy từ Entity (Map) -> DTO (List)
    @Mapping(target = "headers", source = "headers", qualifiedByName = "mapHeadersMapToList")
    @Mapping(target = "queryParams", source = "queryParams", qualifiedByName = "mapHeadersMapToList")
    ApiResponse toResponse(Monitor entity);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    // Chạy từ DTO (List) -> Entity (Map)
    @Mapping(target = "headers", source = "headers", qualifiedByName = "mapHeadersListToMap")
    @Mapping(target = "queryParams", source = "queryParams", qualifiedByName = "mapHeadersListToMap")
    void updateEntityFromDto(UpdateApiRequest updateRequest, @MappingTarget Monitor entity);

    @Named("mapHeadersListToMap")
    default Map<String, String> mapHeadersListToMap(List<Map<String, String>> headersList) {
        if (headersList == null)
            return new HashMap<>();
        return headersList.stream()
                .filter(h -> h.get("key") != null && !h.get("key").isBlank())
                .collect(Collectors.toMap(
                        h -> h.get("key"),
                        h -> h.get("value") == null ? "" : h.get("value"),
                        (existing, replacement) -> existing));
    }

    @Named("mapHeadersMapToList")
    default List<Map<String, String>> mapHeadersMapToList(Map<String, String> headersMap) {
        if (headersMap == null)
            return null;
        return headersMap.entrySet().stream()
                .map(entry -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("key", entry.getKey());
                    map.put("value", entry.getValue());
                    return map;
                })
                .collect(Collectors.toList());
    }

    default AdminUserMonitorStatsDto mapObjectToStats(Object[] result) {
        if (result == null || result.length < 1) {
            return new AdminUserMonitorStatsDto(0, 0);
        }

        // Với một số trường hợp, Hibernate trả về kết quả là một mảng lồng nhau (Object[][])
        Object[] row = (result[0] instanceof Object[]) ? (Object[]) result[0] : result;

        // Ép kiểu thông qua lớp Number để đảm bảo an toàn (Long -> int)
        int total = row.length > 0 && row[0] != null ? ((Number) row[0]).intValue() : 0;
        int active = row.length > 1 && row[1] != null ? ((Number) row[1]).intValue() : 0;

        return new AdminUserMonitorStatsDto(total, active);
    }

}