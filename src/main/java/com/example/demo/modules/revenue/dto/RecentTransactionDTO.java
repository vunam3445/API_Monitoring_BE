package com.example.demo.modules.revenue.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class RecentTransactionDTO {
    private UUID id;
    private String userName;
    private String userEmail;
    private BigDecimal amount;
    private String plan;
    private LocalDateTime date;
    private String status;
}
