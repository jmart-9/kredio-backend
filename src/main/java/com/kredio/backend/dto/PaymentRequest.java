package com.kredio.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(
        @NotNull(message = "El ID del préstamo es obligatorio")
        UUID loanId,

        @NotNull(message = "El ID del cobrador es obligatorio")
        UUID collectorId,

        @NotNull(message = "El monto pagado es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
        BigDecimal amountPaid,

        @NotNull(message = "El método de pago es obligatorio")
        String method,

        Boolean isPrinted,
        Boolean syncedFromOffline
) {
}