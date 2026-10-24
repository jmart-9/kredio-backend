package com.kredio.backend.engine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlatAmortizationCalculator implements AmortizationEngine {

    private final RateConverter rateConverter;
    private final DayCountCalculator dayCountCalculator;
    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);

    @Override
    public List<AmortizationScheduleDTO> generateSchedule(AmortizationRequest request) {
        List<AmortizationScheduleDTO> schedule = new ArrayList<>();

        BigDecimal principal = request.getPrincipalAmount();
        BigDecimal periodicRate = rateConverter.getPeriodicRate(
                request.getAnnualInterestRate(),
                request.getRateType(),
                request.getPaymentFrequency()
        );

        int totalPeriods = calculateTotalPeriods(request);

        // Interés total calculado al inicio sobre el 100% del capital
        BigDecimal totalInterest = principal.multiply(periodicRate, MC).multiply(new BigDecimal(totalPeriods));

        // División equitativa entre períodos
        BigDecimal fixedPrincipal = principal.divide(new BigDecimal(totalPeriods), MC);
        BigDecimal fixedInterest = totalInterest.divide(new BigDecimal(totalPeriods), MC);
        BigDecimal fixedInstallment = fixedPrincipal.add(fixedInterest);

        BigDecimal remainingBalance = principal;

        for (int i = 1; i <= totalPeriods; i++) {
            // Ajuste de redondeo en la última cuota
            BigDecimal currentPrincipal = (i == totalPeriods) ? remainingBalance : fixedPrincipal;
            BigDecimal currentInterest = fixedInterest;

            remainingBalance = remainingBalance.subtract(currentPrincipal);
            if (remainingBalance.compareTo(BigDecimal.ZERO) < 0) {
                remainingBalance = BigDecimal.ZERO;
            }

            LocalDate dueDate = dayCountCalculator.calculateDueDate(
                    request.getStartDate(), i, request.getPaymentFrequency()
            );

            if (request.isAdjustForBusinessDays()) {
                dueDate = dayCountCalculator.adjustToBusinessDay(dueDate);
            }

            int daysCount = dayCountCalculator.calculateDays(
                    request.getStartDate().plusMonths(i - 1),
                    dueDate,
                    request.getDayCountConvention()
            );

            schedule.add(AmortizationScheduleDTO.builder()
                    .installmentNumber(i)
                    .dueDate(dueDate)
                    .installmentAmount(fixedInstallment.setScale(4, RoundingMode.HALF_UP))
                    .principalAmount(currentPrincipal.setScale(4, RoundingMode.HALF_UP))
                    .interestAmount(currentInterest.setScale(4, RoundingMode.HALF_UP))
                    .remainingBalance(remainingBalance.setScale(4, RoundingMode.HALF_UP))
                    .daysCount(daysCount)
                    .adjustedForBusinessDay(false)
                    .build());
        }

        log.info("✅ Calendario Flat generado: {} cuotas de {} (Interés total: {})",
                totalPeriods, fixedInstallment, totalInterest);
        return schedule;
    }

    private int calculateTotalPeriods(AmortizationRequest request) {
        int months = request.getTermMonths();
        return switch (request.getPaymentFrequency()) {
            case DAILY -> months * 30;
            case WEEKLY -> months * 4;
            case BIWEEKLY -> months * 2;
            case SEMIMONTHLY -> months * 2;
            case MONTHLY -> months;
            case BIMONTHLY -> months / 2;
            case QUARTERLY -> months / 3;
            case SEMIANNUAL -> months / 6;
            case ANNUAL -> months / 12;
        };
    }

    @Override
    public AmortizationMethod getMethod() {
        return AmortizationMethod.FLAT;
    }
}