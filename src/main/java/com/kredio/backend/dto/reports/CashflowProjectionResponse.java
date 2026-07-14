package com.kredio.backend.dto.reports;

import java.math.BigDecimal;

public record CashflowProjectionResponse(
        BigDecimal expectedNext30Days,    // Cuotas que vencen en próximos 30 días
        BigDecimal expectedNext60Days,    // Cuotas que vencen en próximos 60 días
        BigDecimal expectedNext90Days,    // Cuotas que vencen en próximos 90 días
        int pendingSchedules30,           // Cantidad de cuotas próximas a vencer (30d)
        int pendingSchedules60,
        int pendingSchedules90,
        BigDecimal overdueAmount,         // Monto de cuotas YA vencidas no pagadas
        int overdueSchedulesCount         // Cantidad de cuotas vencidas no pagadas
) {
}