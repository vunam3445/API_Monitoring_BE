package com.example.demo.modules.payment.services;

import com.example.demo.common.config.VNPayConfig;
import com.example.demo.common.exceptions.AuthenticationException;
import com.example.demo.common.exceptions.ResourceNotFoundException;
import com.example.demo.common.security.ISecurityContextService;
import com.example.demo.modules.payment.dto.CreatePaymentRequest;
import com.example.demo.modules.payment.dto.PaymentResponse;
import com.example.demo.modules.subscription.entities.Subscription;
import com.example.demo.modules.subscription.entities.SubscriptionPlan;
import com.example.demo.modules.subscription.enums.BillingCycle;
import com.example.demo.modules.subscription.enums.SubscriptionStatus;
import com.example.demo.modules.subscription.repositories.SubscriptionPlanRepository;
import com.example.demo.modules.subscription.repositories.SubscriptionRepository;
import com.example.demo.modules.paymentLogs.entities.PaymentLogs;
import com.example.demo.modules.paymentLogs.enums.PaymentStatus;
import com.example.demo.modules.paymentLogs.repositories.PaymentLogsRepository;
import com.example.demo.modules.user.entities.User;
import com.example.demo.modules.user.repositories.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VNPayService implements IPaymentService {

    private final VNPayConfig vnPayConfig;
    private final ISecurityContextService iSecurityContextService;
    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final PaymentLogsRepository paymentLogsRepository;

    @Override
    public PaymentResponse createPaymentUrl(CreatePaymentRequest request, HttpServletRequest servletRequest) {
        User user = iSecurityContextService.getCurrentUser()
                .orElseThrow(() -> new AuthenticationException("Bạn chưa đăng nhập."));

        SubscriptionPlan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Gói đăng ký không tồn tại."));

        // 1. Tính toán giá tiền thực tế dựa trên đơn vị tiền tệ
        BigDecimal finalPrice = plan.getPrice();
        if ("USD".equalsIgnoreCase(plan.getCurrency())) {
            finalPrice = finalPrice.multiply(BigDecimal.valueOf(25000)); // Tạm tính 1 USD = 25,000 VND
        }

        // 2. VNPay yêu cầu số tiền nhân 100 (vnp_Amount tính theo đơn vị nhỏ nhất, VND không có xu nên là n * 100)
        long amount = finalPrice.multiply(BigDecimal.valueOf(100)).longValue();

        String vnp_TxnRef = vnPayConfig.getRandomNumber(8);
        String vnp_IpAddr = vnPayConfig.getIpAddress(servletRequest);

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnPayConfig.getVnp_Version());
        vnp_Params.put("vnp_Command", vnPayConfig.getVnp_Command());
        vnp_Params.put("vnp_TmnCode", vnPayConfig.getVnp_TmnCode());
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");

        if (request.getBankCode() != null && !request.getBankCode().isEmpty()) {
            vnp_Params.put("vnp_BankCode", request.getBankCode());
        }
        
        // Đưa planId và userId vào OrderInfo để hứng lại ở phần callback
        String orderInfo = "UserID:" + user.getId() + "_PlanID:" + plan.getId();
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", orderInfo);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getVnp_ReturnUrl());
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                // Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (i < fieldNames.size() - 1) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String queryUrl = query.toString();
        String vnp_SecureHash = vnPayConfig.hmacSHA512(vnPayConfig.getSecretKey(), hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = vnPayConfig.getVnp_PayUrl() + "?" + queryUrl;

        // 3. Kiểm tra xem có giao dịch PENDING nào không để tái sử dụng, tránh rác DB
        PaymentLogs paymentLog = paymentLogsRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), PaymentStatus.PENDING)
                .orElse(new PaymentLogs());

        paymentLog.setUser(user);
        paymentLog.setAmount(plan.getPrice());
        paymentLog.setPlanName(plan.getName());
        paymentLog.setStatus(PaymentStatus.PENDING);
        paymentLog.setTransactionId(vnp_TxnRef);
        paymentLog.setPaymentMethod("VNPAY");
        paymentLog.setCurrency("VND");
        paymentLog.setSubscription(subscriptionRepository.findByUserIdAndStatus(user.getId(), SubscriptionStatus.ACTIVE).orElse(null));

        paymentLogsRepository.save(paymentLog);

        return PaymentResponse.builder()
                .paymentUrl(paymentUrl)
                .build();
    }

    @Override
    @Transactional
    public boolean handlePaymentCallback(Map<String, String> requestParams) {
        // Hash lại requestParams xem có đúng với vnp_SecureHash trả về không
        Map<String, String> fields = new HashMap<>();
        for (Map.Entry<String, String> entry : requestParams.entrySet()) {
            if (entry.getValue() != null && entry.getValue().length() > 0) {
                fields.put(entry.getKey(), entry.getValue());
            }
        }

        String vnp_SecureHash = fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");

        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();

        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            String fieldValue = fields.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (i < fieldNames.size() - 1) {
                    hashData.append('&');
                }
            }
        }

        String signValue = vnPayConfig.hmacSHA512(vnPayConfig.getSecretKey(), hashData.toString());

        String vnp_TxnRef = requestParams.get("vnp_TxnRef");
        PaymentLogs paymentLog = paymentLogsRepository.findByTransactionId(vnp_TxnRef)
                .orElse(null);

        if (signValue.equals(vnp_SecureHash)) {
            if ("00".equals(requestParams.get("vnp_TransactionStatus"))) {
                // Giao dịch thành công, phân tích OrderInfo để lấy UserID và PlanID
                String orderInfo = requestParams.get("vnp_OrderInfo");
                // orderInfo format: "UserID:UUID_PlanID:UUID"
                
                try {
                    String[] parts = orderInfo.split("_");
                    String userIdStr = parts[0].replace("UserID:", "");
                    String planIdStr = parts[1].replace("PlanID:", "");

                    UUID userId = UUID.fromString(userIdStr);
                    UUID planId = UUID.fromString(planIdStr);

                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

                    SubscriptionPlan plan = planRepository.findById(planId)
                            .orElseThrow(() -> new ResourceNotFoundException("Plan không tồn tại"));

                    // Kích hoạt gói vào User
                    user.setSubscriptionPlan(plan);
                    user.setPlanType(plan.getName());

                    // Cập nhật hoặc tạo Subscription mới
                    Subscription subscription = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                            .orElse(new Subscription());

                    subscription.setUser(user);
                    subscription.setPlan(plan);
                    subscription.setPlanName(plan.getName());
                    subscription.setPlanPrice(plan.getPrice());
                    subscription.setMaxMonitors(plan.getMaxMonitors());
                    subscription.setMinInterval(plan.getMinInterval());
                    subscription.setStartDate(LocalDateTime.now());
                    subscription.setCurrentPeriodEnd(LocalDateTime.now().plusMonths(1));
                    subscription.setBillingCycle(BillingCycle.MONTHLY);
                    subscription.setStatus(SubscriptionStatus.ACTIVE);
                    subscription.setPaymentStatus(PaymentStatus.PAID);

                    userRepository.save(user);
                    subscriptionRepository.save(subscription);

                    // Cập nhật PaymentLog thành công
                    if (paymentLog != null) {
                        paymentLog.setStatus(PaymentStatus.SUCCESS);
                        paymentLog.setSubscription(subscription); // Gắn subscription vào log nếu trước đó là null
                        paymentLogsRepository.save(paymentLog);
                    }

                    log.info("Thanh toán thành công. Kích hoạt gói {} cho user {}", plan.getName(), user.getEmail());
                    return true;
                } catch (Exception e) {
                    log.error("Lỗi khi xử lý dữ liệu sau thanh toán: ", e);
                    // Có thể cân nhắc update log thành FAILED ở đây nếu cần
                    return false;
                }
            } else {
                // Giao dịch thất bại từ phía VNPay
                if (paymentLog != null) {
                    paymentLog.setStatus(PaymentStatus.FAILED);
                    paymentLogsRepository.save(paymentLog);
                }
                log.warn("Giao dịch VNPay thất bại mã: {}", requestParams.get("vnp_TransactionStatus"));
            }
        }
        return false;
    }
}
