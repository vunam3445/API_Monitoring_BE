package com.example.demo.modules.paymentLogs.services;

import com.example.demo.common.base.IReadOnlyService;
import com.example.demo.modules.paymentLogs.dto.PaymentLogsResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface IPaymentLogsService extends IReadOnlyService<PaymentLogsResponse, UUID> {
    Page<PaymentLogsResponse> findByUserId(Pageable pageable);
}
