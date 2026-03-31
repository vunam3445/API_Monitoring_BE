package com.example.demo.modules.payment.controllers;

import com.example.demo.common.security.annotations.IsAuthenticated;
import com.example.demo.modules.payment.dto.CreatePaymentRequest;
import com.example.demo.modules.payment.dto.PaymentResponse;
import com.example.demo.modules.payment.services.IPaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final IPaymentService paymentService;

    @PostMapping("/create-url")
    @IsAuthenticated
    public ResponseEntity<PaymentResponse> createPaymentUrl(
            @RequestBody CreatePaymentRequest request,
            HttpServletRequest servletRequest) {
        
        PaymentResponse response = paymentService.createPaymentUrl(request, servletRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/vnpay-return")
    public ResponseEntity<?> handleVNPayReturn(HttpServletRequest request) {
        // Collect all parameters
        Map<String, String> fields = new HashMap<>();
        for (String paramName : request.getParameterMap().keySet()) {
            String paramValue = request.getParameter(paramName);
            fields.put(paramName, paramValue);
        }

        boolean success = paymentService.handlePaymentCallback(fields);
        
        // Trả về response dạng JSON để frontend xử lý redirect
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "Thanh toán thành công" : "Thanh toán thất bại hoặc dữ liệu không hợp lệ");
        
        return ResponseEntity.ok(result);
    }
}
