package com.example.demo.modules.payment.services;

import com.example.demo.modules.payment.dto.CreatePaymentRequest;
import com.example.demo.modules.payment.dto.PaymentResponse;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

public interface IPaymentService {
    PaymentResponse createPaymentUrl(CreatePaymentRequest request, HttpServletRequest servletRequest);
    boolean handlePaymentCallback(Map<String, String> params);
}
