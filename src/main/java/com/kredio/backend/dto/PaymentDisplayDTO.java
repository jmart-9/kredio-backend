package com.kredio.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentDisplayDTO(
        UUID id,
        UUID scheduleId,
        Integer installmentNumber,
        BigDecimal expectedAmount,
        BigDecimal paidAmount,
        BigDecimal excessAmount,
        BigDecimal appliedToPrincipal,
        String status,
        LocalDate paymentDate,
        String collectorName
) {
    // Método helper para mostrar el pago real
    public BigDecimal getTotalPaid() {
        return paidAmount != null ? paidAmount.add(excessAmount != null ? excessAmount : BigDecimal.ZERO) : BigDecimal.ZERO;
    }
}
