package com.example.demo.modules.subscription.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ManualRenewalRequest {
    @NotNull(message = "UserId không được để trống")
    private UUID userId;

    @NotNull(message = "Số tháng gia hạn không được để trống")
    @Min(value = 1, message = "Gia hạn tối thiểu 1 tháng")
    private Integer durationMonths;
    @NotNull(message = "Lý do gia hạn không được để trống")
    private String note; // Ghi chú nội bộ
    private BigDecimal amount = BigDecimal.ZERO;
}
