package com.kredio.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioResponse {
    private UUID id;
    private String name;
    private String description;
    private UUID assignedUserId;
    private String assignedUserName;
    private Boolean isActive;
    private Integer totalClients;
    private Integer activeLoans;
    private BigDecimal totalBalance;
    private Instant createdAt;
}