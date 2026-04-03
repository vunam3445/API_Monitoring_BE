package com.example.demo.modules.alert.dto;

import com.example.demo.modules.alert.enums.AlertChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAlertConfigRequest {
    @NotNull(message = "Kênh alert không được để trống")
    private AlertChannelType type;

    @NotBlank(message = "Điểm đến không được để trống (Email hoặc Webhook URL)")
    private String destination;

    @Builder.Default
    private Boolean isEnabled = true;
}
