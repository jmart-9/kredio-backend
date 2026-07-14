package com.kredio.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanScheduleResponse {
    private UUID id;
    private Integer installmentNumber;
    private LocalDate dueDate;
    private BigDecimal expectedAmount;
    private BigDecimal expectedPrincipal;
    private BigDecimal expectedInterest;
    private String status;
}