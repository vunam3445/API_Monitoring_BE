package com.example.demo.modules.feature.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateFeatureRequest {
    @NotBlank(message = "Key tính năng không được để trống")
    @Size(max = 100, message = "Key tính năng không được vượt quá 100 ký tự")
    @Pattern(regexp = "^[a-z0-9_]+$", message = "Key tính năng chỉ được chứa chữ thường, số và dấu gạch dưới (snake_case)")
    private String key;

    @NotBlank(message = "Tên hiển thị không được để trống")
    @Size(max = 255, message = "Tên hiển thị không được vượt quá 255 ký tự")
    private String label;

    private String description;

    private Boolean isActive = true;
}
