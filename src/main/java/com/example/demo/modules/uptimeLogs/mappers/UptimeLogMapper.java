package com.example.demo.modules.uptimeLogs.mappers;

import com.example.demo.modules.uptimeLogs.dto.UptimeLogResponse;
import com.example.demo.modules.uptimeLogs.entities.UptimeLogs;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

/**
 * MapStruct mapper cho UptimeLogs.
 *
 * Vì UptimeLogs là read-only (Worker ghi trực tiếp entity, không cần DTO đầu vào),
 * mapper này chỉ cần convert Entity → Response DTO.
 */
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UptimeLogMapper {

    /**
     * Chuyển Entity sang Response DTO.
     * MapStruct tự mapping theo tên field.
     */
    @org.mapstruct.Mapping(target = "monitorName", source = "monitor.name")
    @org.mapstruct.Mapping(target = "monitorUrl", source = "monitor.url")
    @org.mapstruct.Mapping(target = "monitorMethod", source = "monitor.method")
    UptimeLogResponse toResponse(UptimeLogs entity);

    /**
     * Chuyển danh sách Entity sang danh sách Response DTO.
     */
    List<UptimeLogResponse> toResponseList(List<UptimeLogs> entities);
}

