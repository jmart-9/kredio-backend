package com.kredio.backend.engine;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class AmortizationRequest {
    private BigDecimal principalAmount;
    private BigDecimal annualInterestRate;
    private int termMonths;
    private PaymentFrequency paymentFrequency;
    private AmortizationMethod amortizationMethod;
    private RateType rateType;
    private DayCountConvention dayCountConvention;
    private EarlyRepaymentPolicy earlyRepaymentPolicy;
    private LocalDate startDate;
    private int graceDays;
    private boolean adjustForBusinessDays;
}