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

        // ✅ NUEVO: Portfolio/Cartera
        UUID portfolioId,

        @NotNull(message = "El monto principal es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto principal debe ser mayor a cero")
        BigDecimal principalAmount,

        @NotNull(message = "La tasa de interés es obligatoria")
        @DecimalMin(value = "0.0", message = "La tasa de interés no puede ser negativa")
        BigDecimal interestRate,

        @NotNull(message = "El tipo de tasa es obligatorio")
        String rateType,

        @NotNull(message = "La frecuencia de pago es obligatoria")
        String paymentFrequency,

        @NotNull(message = "El método de amortización es obligatorio")
        String amortizationMethod,

        @NotNull(message = "El número de cuotas es obligatorio")
        @Min(value = 1, message = "El número de cuotas debe ser al menos 1")
        Integer term,

        @NotNull(message = "La fecha de apertura es obligatoria")
        LocalDate openingDate,

        // ✅ NUEVO: Fecha del primer pago
        LocalDate firstPaymentDate,

        String dayCountConvention,
        String earlyRepaymentPolicy,
        Integer graceDays,
        Boolean adjustForBusinessDays,

        // ✅ NUEVOS CAMPOS para la especificación
        String disbursementMethod,
        BigDecimal administrativeFee,
        String feeCollectionMode,
        String moratoryType,
        BigDecimal moratoryValue
) {}