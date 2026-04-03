package com.example.demo.modules.paymentLogs.mappers;

import com.example.demo.modules.paymentLogs.dto.PaymentLogsResponse;
import com.example.demo.modules.paymentLogs.entities.PaymentLogs;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PaymentLogsMapper {

    @Mapping(target = "userId", source = "user.id")
    PaymentLogsResponse toResponse(PaymentLogs entity);
}
