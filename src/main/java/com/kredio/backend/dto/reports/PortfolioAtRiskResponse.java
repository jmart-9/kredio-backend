package com.kredio.backend.dto.reports;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PortfolioAtRiskResponse(
        LocalDate reportDate,
        BigDecimal totalPortfolio,        // Saldo total de cartera activa
        BigDecimal par30Amount,           // Saldo en riesgo > 30 días
        BigDecimal par60Amount,           // Saldo en riesgo > 60 días
        BigDecimal par90Amount,           // Saldo en riesgo > 90 días
        BigDecimal par30Percentage,       // % PAR 30
        BigDecimal par60Percentage,       // % PAR 60
        BigDecimal par90Percentage,       // % PAR 90
        int totalActiveLoans,             // Total préstamos activos
        int loansWithOverdue30,           // Préstamos con mora > 30 días
        int loansWithOverdue60,           // Préstamos con mora > 60 días
        int loansWithOverdue90            // Préstamos con mora > 90 días
) {
}