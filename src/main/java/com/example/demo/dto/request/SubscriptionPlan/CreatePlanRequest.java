package com.example.demo.dto.request.SubscriptionPlan;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreatePlanRequest {
    @NotBlank(message = "Tên gói không được để trống")
    private String name;

    private String description;

    @NotNull(message = "Giá không được để trống")
    @Min(value = 0, message = "Giá không được nhỏ hơn 0")
    private Double price;

    private String currency = "USD";

    @NotNull(message = "Số lượng monitor tối đa không được để trống")
    @Min(value = 1, message = "Phải có ít nhất 1 monitor")
    private Integer maxMonitors;

    @NotNull(message = "Interval tối thiểu không được để trống")
    @Min(value = 60, message = "Interval tối thiểu là 60 giây")
    private Integer minInterval;

    private String features; // Chuỗi JSON

    private Boolean isActive = true;
}