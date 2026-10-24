package com.kredio.backend.engine;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class AmortizationScheduleDTO {
    private int installmentNumber;
    private LocalDate dueDate;
    private BigDecimal installmentAmount;      // Cuota total
    private BigDecimal principalAmount;        // Capital de la cuota
    private BigDecimal interestAmount;         // Interés de la cuota
    private BigDecimal remainingBalance;       // Saldo insoluto
    private int daysCount;                     // Días del período
    private boolean adjustedForBusinessDay;
}