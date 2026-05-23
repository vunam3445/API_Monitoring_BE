package com.example.demo.modules.paymentLogs.entities;

import com.example.demo.modules.subscription.entities.Subscription;
import com.example.demo.modules.user.entities.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import com.example.demo.modules.paymentLogs.enums.PaymentStatus;
@Entity
@Table(name = "payment_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentLogs {

    /**
     * Khóa chính UUID của bảng payment_logs
     */
    @Id
    @GeneratedValue
    private UUID id;

    /**
     * User thực hiện thanh toán
     * Nhiều payment log có thể thuộc về 1 user
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Subscription liên quan đến thanh toán
     * Có thể null nếu log này chưa gắn với subscription cụ thể
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    /**
     * Số tiền thanh toán
     * numeric(12,2) trong DB -> BigDecimal trong Java
     */
    @Column(name = "amount", nullable = true, precision = 12, scale = 2)
    private BigDecimal amount;

    /**
     * Loại tiền tệ, mặc định là VND
     */
    @Builder.Default
    @Column(name = "currency", nullable = true, length = 10)
    private String currency = "VND";

    /**
     * Tên gói tại thời điểm thanh toán (Snapshot)
     */
    @Column(name = "plan_name")
    private String planName;

    /**
     * Mã giao dịch từ cổng thanh toán
     * Ví dụ: Stripe transaction id, VNPay txnRef...
     */
    @Column(name = "transaction_id")
    private String transactionId;

    /**
     * Phương thức thanh toán
     * Ví dụ: CARD, PAYPAL, VNPAY, MOMO...
     */
    @Column(name = "payment_method")
    private String paymentMethod;

    /**
     * Mã hóa đơn nếu có
     */
    @Column(name = "invoice_id")
    private String invoiceId;

    /**
     * Trạng thái thanh toán
     * SUCCESS / FAILED / PENDING
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = true)
    private PaymentStatus status;

    /**
     * Thời điểm record được tạo
     * Hibernate tự set khi insert
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Ghi chú chi tiết cho giao dịch
     **/
    @Column(name = "notes", length = 500)
    private String notes;
}