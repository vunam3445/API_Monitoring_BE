package com.example.demo.modules.paymentLogs.services;

import com.example.demo.common.exceptions.AuthenticationException;
import com.example.demo.common.security.ISecurityContextService;
import com.example.demo.modules.paymentLogs.dto.PaymentLogsResponse;
import com.example.demo.modules.paymentLogs.mappers.PaymentLogsMapper;
import com.example.demo.modules.paymentLogs.repositories.PaymentLogsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentLogsService implements IPaymentLogsService {
    private final PaymentLogsRepository paymentLogsRepository;
    private final PaymentLogsMapper paymentLogsMapper;
    private final ISecurityContextService iSecurityContextService;

    public PaymentLogsService(PaymentLogsRepository paymentLogsRepository, 
                              PaymentLogsMapper paymentLogsMapper, 
                              ISecurityContextService iSecurityContextService) {
        this.paymentLogsRepository = paymentLogsRepository;
        this.paymentLogsMapper = paymentLogsMapper;
        this.iSecurityContextService = iSecurityContextService;
    }

    @Override
    public PaymentLogsResponse findById(UUID uuid) {
        return paymentLogsRepository.findById(uuid)
                .map(paymentLogsMapper::toResponse)
                .orElse(null);
    }

    @Override
    public Page<PaymentLogsResponse> findAll(Pageable pageable) {
        return paymentLogsRepository.findAll(pageable)
                .map(paymentLogsMapper::toResponse);
    }

    @Override
    public Page<PaymentLogsResponse> findByUserId(Pageable pageable) {
        UUID userId = iSecurityContextService.getCurrentUserId()
                .orElseThrow(() -> new AuthenticationException("Bạn chưa đăng nhập."));

        return paymentLogsRepository.findAllByUserId(userId, pageable)
                .map(paymentLogsMapper::toResponse);
    }
}
