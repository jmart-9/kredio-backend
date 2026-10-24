package com.kredio.backend.engine;

public enum PaymentFrequency {
    DAILY(360),
    WEEKLY(52),
    BIWEEKLY(26),       // Catorcenal
    SEMIMONTHLY(24),    // Quincenal
    MONTHLY(12),
    BIMONTHLY(6),       // Bimestral
    QUARTERLY(4),       // Trimestral
    SEMIANNUAL(2),      // Semestral
    ANNUAL(1);

    private final int periodsPerYear;

    PaymentFrequency(int periodsPerYear) {
        this.periodsPerYear = periodsPerYear;
    }

    public int getPeriodsPerYear() {
        return periodsPerYear;
    }
}