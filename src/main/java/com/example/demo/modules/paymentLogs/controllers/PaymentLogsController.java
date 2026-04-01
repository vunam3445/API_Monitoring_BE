package com.example.demo.modules.paymentLogs.controllers;

import com.example.demo.common.base.BaseController;
import com.example.demo.modules.paymentLogs.dto.PaymentLogsResponse;
import com.example.demo.modules.paymentLogs.services.IPaymentLogsService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("api/payment-logs")
public class PaymentLogsController {
    
    private final IPaymentLogsService iPaymentLogsService;

    public PaymentLogsController(IPaymentLogsService iPaymentLogsService) {
        this.iPaymentLogsService = iPaymentLogsService;
    }

    /**
     * Lấy lịch sử giao dịch của người dùng hiện tại
     * Tự động lấy userId từ SecurityContext
     */
    @GetMapping
    public ResponseEntity<?> getMyPaymentLogs(Pageable pageable){
        return ResponseEntity.ok(iPaymentLogsService.findByUserId(pageable));
    }

    /**
     * Lấy chi tiết một giao dịch theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPaymentLogsDetail(@PathVariable UUID id){
        PaymentLogsResponse response = iPaymentLogsService.findById(id);
        return ResponseEntity.ok(response);
    }
}
