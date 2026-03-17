package com.example.demo.dto.response.SubscriptionPlan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private UUID id;
    private String name;
    private String description;
    private Double price;
    private String currency;
    private Integer maxMonitors;
    private Integer minInterval;

    // Trả về String JSON hoặc bạn có thể dùng Object/Map tùy vào cách bạn muốn React nhận dữ liệu
    private String features;

    private Boolean isActive;

    // Lưu ý: Chúng ta KHÔNG bao gồm trường isDelete ở đây
    // vì Client không cần biết bản ghi đã đánh dấu xóa hay chưa.
}