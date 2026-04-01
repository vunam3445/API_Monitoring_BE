package com.example.demo.modules.subscription.entities;

import com.example.demo.modules.subscription.enums.BillingCycle;
import com.example.demo.modules.subscription.enums.SubscriptionStatus;
import com.example.demo.modules.user.entities.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    /**
     * Khóa chính của subscription
     */
    @Id
    @GeneratedValue
    private UUID id;

    /**
     * User sở hữu subscription này
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Gói gốc được chọn từ subscription_plans
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    /**
     * Snapshot tên gói tại thời điểm user đăng ký/mua
     * Để sau này plan đổi tên thì subscription cũ vẫn giữ dữ liệu cũ
     */
    @Column(name = "plan_name", nullable = true)
    private String planName;

    /**
     * Snapshot giá gói tại thời điểm mua
     */
    @Column(name = "plan_price", nullable = true, precision = 12, scale = 2)
    private BigDecimal planPrice;

    /**
     * Đơn vị tiền tệ
     */
    @Builder.Default
    @Column(name = "currency", nullable = true, length = 10)
    private String currency = "VND";

    /**
     * Snapshot số monitor tối đa user được dùng ở thời điểm đăng ký
     */
    @Column(name = "max_monitors", nullable = true)
    private Integer maxMonitors;

    /**
     * Snapshot khoảng thời gian check nhỏ nhất cho phép
     */
    @Column(name = "min_interval", nullable = true)
    private Integer minInterval;
    /**
     * Ngày bắt đầu hiệu lực gói
     */
    @Column(name = "start_date", nullable = true)
    private LocalDateTime startDate;

    /**
     * Ngày kết thúc chu kỳ hiện tại
     * Dùng để xác định khi nào hết hạn và gửi email nhắc gia hạn
     */
    @Column(name = "current_period_end", nullable = true)
    private LocalDateTime currentPeriodEnd;

    /**
     * Chu kỳ thanh toán: MONTHLY hoặc YEARLY
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = true)
    private BillingCycle billingCycle;

    /**
     * Trạng thái subscription: ACTIVE, EXPIRED, CANCELED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = true)
    private SubscriptionStatus status;

    /**
     * Trạng thái thanh toán: PAID, PENDING, FAILED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = true)
    private com.example.demo.modules.paymentLogs.enums.PaymentStatus paymentStatus;

    /**
     * Thời điểm tạo bản ghi
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Thời điểm cập nhật bản ghi gần nhất
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}