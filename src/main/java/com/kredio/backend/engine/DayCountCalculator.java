package com.kredio.backend.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
public class DayCountCalculator {

    /**
     * Calcula los días entre dos fechas según la convención
     */
    public int calculateDays(LocalDate startDate, LocalDate endDate, DayCountConvention convention) {
        return switch (convention) {
            case ACTUAL_360, ACTUAL_365 -> (int) ChronoUnit.DAYS.between(startDate, endDate);
            case THIRTY_360 -> calculateThirty360(startDate, endDate);
        };
    }

    /**
     * Convención 30/360 (European)
     * Cada mes tiene 30 días, el año 360 días
     */
    private int calculateThirty360(LocalDate startDate, LocalDate endDate) {
        int d1 = Math.min(startDate.getDayOfMonth(), 30);
        int d2 = (d1 == 30) ? Math.min(endDate.getDayOfMonth(), 30) : endDate.getDayOfMonth();
        int m1 = startDate.getMonthValue();
        int m2 = endDate.getMonthValue();
        int y1 = startDate.getYear();
        int y2 = endDate.getYear();

        return 360 * (y2 - y1) + 30 * (m2 - m1) + (d2 - d1);
    }

    /**
     * Calcula el factor de año según la convención
     */
    public double getYearFraction(DayCountConvention convention) {
        return switch (convention) {
            case ACTUAL_360 -> 360.0;
            case ACTUAL_365 -> 365.0;
            case THIRTY_360 -> 360.0;
        };
    }

    /**
     * Ajusta la fecha al siguiente día hábil si cae en fin de semana
     */
    public LocalDate adjustToBusinessDay(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY) {
            return date.plusDays(2); // Lunes
        } else if (day == DayOfWeek.SUNDAY) {
            return date.plusDays(1); // Lunes
        }
        return date;
    }

    /**
     * Calcula la fecha de vencimiento según la frecuencia
     */
    public LocalDate calculateDueDate(LocalDate startDate, int installmentNumber, PaymentFrequency frequency) {
        return switch (frequency) {
            case DAILY -> startDate.plusDays(installmentNumber);
            case WEEKLY -> startDate.plusWeeks(installmentNumber);
            case BIWEEKLY -> startDate.plusWeeks(installmentNumber * 2);
            case SEMIMONTHLY -> startDate.plusDays(installmentNumber * 15);
            case MONTHLY -> startDate.plusMonths(installmentNumber);
            case BIMONTHLY -> startDate.plusMonths(installmentNumber * 2);
            case QUARTERLY -> startDate.plusMonths(installmentNumber * 3);
            case SEMIANNUAL -> startDate.plusMonths(installmentNumber * 6);
            case ANNUAL -> startDate.plusYears(installmentNumber);
        };
    }
}