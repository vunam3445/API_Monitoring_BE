package com.example.demo.modules.monitor.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.validator.constraints.URL;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateApiRequest {

    @NotNull(message = "ID của Monitor không được để trống khi cập nhật")
    private UUID id;

    @NotBlank(message = "Tên monitor không được để trống")
    @Size(max = 255, message = "Tên không được vượt quá 255 ký tự")
    private String name;

    @NotBlank(message = "URL không được để trống")
    @URL(message = "Định dạng URL không hợp lệ")
    private String url;

    @NotBlank(message = "Method không được để trống")
    @Pattern(regexp = "^(GET)$", message = "Hiện tại hệ thống chỉ hỗ trợ phương thức GET")
    private String method;

    private Map<String, Object> auth;

    private List<Map<String, String>> headers;

    private List<Map<String, String>> queryParams;

    private String body;

    @NotNull(message = "Khoảng thời gian kiểm tra không được để trống")
    @Min(value = 10, message = "Khoảng thời gian tối thiểu là 10 giây")
    @Max(value = 86400, message = "Khoảng thời gian tối đa là 24 giờ")
    private Integer checkInterval;

    @NotBlank(message = "Mã trạng thái mong đợi không được để trống")
    @Pattern(regexp = "^[0-9,\\s]+$", message = "Mã trạng thái phải là số, cách nhau bởi dấu phẩy")
    private String expectedStatusCodes;

    @NotNull(message = "Thời gian phản hồi tối đa không được để trống")
    @Positive(message = "Thời gian phản hồi phải là số dương")
    private Integer maxResponseTimeMs;

    // Các trường điều khiển trạng thái (thường dùng cho Update)
    @NotNull(message = "Trạng thái hoạt động (isActive) không được để trống")
    private Boolean isActive;

    @NotNull(message = "Trạng thái thông báo (isMuted) không được để trống")
    private Boolean isMuted;
}