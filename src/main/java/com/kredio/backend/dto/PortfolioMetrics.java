package com.kredio.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioMetrics {
    private BigDecimal totalPortfolio;
    private BigDecimal activePortfolio;
    private BigDecimal overduePortfolio;
    private BigDecimal par30;
    private BigDecimal par60;
    private BigDecimal par90;
    private Integer totalLoans;
    private Integer activeLoans;
    private Integer overdueLoans;
    private Integer completedLoans;
    private BigDecimal defaultRate;
}