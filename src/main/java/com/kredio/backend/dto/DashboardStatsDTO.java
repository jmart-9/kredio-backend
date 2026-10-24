package com.kredio.backend.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record DashboardStatsDTO(
        // Métricas principales
        long totalClients,
        long totalLoans,
        long activeLoans,
        long paidLoans,
        long overdueLoans,
        BigDecimal totalPortfolio,
        BigDecimal activePortfolio,
        BigDecimal overdueAmount,
        BigDecimal collectedAmount,

        // Próximos vencimientos (próximos 7 días)
        BigDecimal upcomingPaymentsAmount,
        int upcomingPaymentsCount,
        List<UpcomingPaymentDTO> upcomingPayments,

        // Datos para gráficos
        List<MonthlyDataDTO> monthlyLoans,
        Map<String, Long> loansByStatus
) {
    public record UpcomingPaymentDTO(
            String loanId,
            String clientName,
            String dueDate,
            BigDecimal amount,
            int installmentNumber
    ) {}

    public record MonthlyDataDTO(
            String month,
            BigDecimal disbursed,
            BigDecimal collected
    ) {}
}