package com.example.demo.modules.subscription.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ManualRenewalRequest {
    @NotNull(message = "Kiểu thời gian không được để trống")
    @Pattern(regexp = "^(day|month|year)$", message = "Kiểu thời gian phải là day, month hoặc year")
    private String type;
    @NotNull(message = "Số tháng gia hạn không được để trống")
    @Min(value = 1, message = "Gia hạn tối thiểu 1 tháng hoặc 1 ngày")
    private Integer time;
    @NotNull(message = "Lý do gia hạn không được để trống")
    private String note; // Ghi chú nội bộ
    private BigDecimal amount = BigDecimal.ZERO;
}
