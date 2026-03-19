package com.example.demo.modules.subscription.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdatePlanRequest {

    // Tên có thể thay đổi, nhưng nếu gửi thì không được để trống hoàn toàn
    private String name;

    private String description;

    @Min(value = 0, message = "Giá không được nhỏ hơn 0")
    private Double price;

    private String currency;

    @Min(value = 1, message = "Phải có ít nhất 1 monitor")
    private Integer maxMonitors;

    @Min(value = 60, message = "Interval tối thiểu là 60 giây")
    private Integer minInterval;

    private String features; // Chuỗi JSON

    private Boolean isActive;
}
