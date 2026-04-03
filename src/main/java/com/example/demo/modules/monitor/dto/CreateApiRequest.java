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
public class CreateApiRequest {

    @NotBlank(message = "Tên monitor không được để trống")
    @Size(max = 255, message = "Tên không được vượt quá 255 ký tự")
    private String name;

    @NotBlank(message = "URL không được để trống")
    @URL(message = "Định dạng URL không hợp lệ")
    private String url;

    @NotBlank(message = "Method không được để trống")
    @Pattern(regexp = "^(GET)$", message = "Hiện tại hệ thống chỉ hỗ trợ phương thức GET")
    private String method;

    // Auth có thể để trống nếu public API
    private Map<String, Object> auth;

    private List<Map<String, String>> headers;

    private List<Map<String, String>> queryParams;

    // Đối với GET, thường body sẽ để trống, bạn có thể thêm logic validate nếu cần
    private String body;

    @NotNull(message = "Khoảng thời gian kiểm tra không được để trống")
    @Min(value = 10, message = "Khoảng thời gian kiểm tra tối thiểu là 10 giây")
    @Max(value = 86400, message = "Khoảng thời gian kiểm tra tối đa là 1 ngày (86400s)")
    private Integer checkInterval;

    @NotBlank(message = "Mã trạng thái mong đợi không được để trống")
    @Pattern(regexp = "^[0-9,\\s]+$", message = "Mã trạng thái phải là số, cách nhau bởi dấu phẩy (VD: 200, 201)")
    private String expectedStatusCodes;

    @NotNull(message = "Thời gian phản hồi tối đa không được để trống")
    @Positive(message = "Thời gian phản hồi phải là số dương")
    private Integer maxResponseTimeMs;

    @NotNull(message = "UserId không được để trống")
    private UUID userId;
}