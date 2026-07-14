package com.kredio.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID loanId,
        UUID collectorId,
        BigDecimal amountPaid,
        BigDecimal previousBalance,
        BigDecimal newBalance,
        Instant paymentDate,
        String method,
        Boolean isPrinted,
        Boolean syncedFromOffline
) {
}