package com.kredio.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LoanResponse(
        UUID id,
        UUID clientId,
        String clientName,
        BigDecimal principalAmount,
        BigDecimal interestRate,
        Integer termMonths,
        String status,
        BigDecimal currentBalance,
        LocalDate openingDate,
        Instant createdAt
) {
}