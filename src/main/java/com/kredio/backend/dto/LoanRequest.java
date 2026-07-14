package com.kredio.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LoanRequest(
        @NotNull(message = "El ID del cliente es obligatorio")
        UUID clientId,

        @NotNull(message = "El monto del préstamo es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
        BigDecimal principalAmount,

        @NotNull(message = "La tasa de interés es obligatoria")
        @DecimalMin(value = "0.0", message = "La tasa de interés no puede ser negativa")
        BigDecimal interestRate,

        @NotNull(message = "El tipo de tasa es obligatorio")
        String rateType, // ANNUAL o MONTHLY

        @NotNull(message = "La modalidad de pago es obligatoria")
        String paymentFrequency, // DAILY, WEEKLY, BIWEEKLY, MONTHLY, QUARTERLY, SEMIANNUAL, ANNUAL

        @NotNull(message = "El método de amortización es obligatorio")
        String amortizationMethod, // FRENCH o SIMPLE

        @NotNull(message = "El plazo es obligatorio")
        @Min(value = 1, message = "El plazo debe ser de al menos 1")
        Integer term,

        @NotNull(message = "La fecha del primer pago es obligatoria")
        LocalDate firstPaymentDate,

        @NotNull(message = "La fecha de apertura es obligatoria")
                LocalDate openingDate
) {
}