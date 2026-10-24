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
public class FrenchAmortizationCalculator implements AmortizationEngine {

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

        // Fórmula de cuota fija: C = P * [i(1+i)^n] / [(1+i)^n - 1]
        BigDecimal fixedInstallment = calculateFixedInstallment(principal, periodicRate, totalPeriods);

        BigDecimal remainingBalance = principal;
        LocalDate currentDate = request.getStartDate();

        for (int i = 1; i <= totalPeriods; i++) {
            BigDecimal interest = remainingBalance.multiply(periodicRate, MC);
            BigDecimal principalPayment = fixedInstallment.subtract(interest);

            // Ajuste de redondeo en la última cuota
            if (i == totalPeriods) {
                principalPayment = remainingBalance;
                fixedInstallment = principalPayment.add(interest);
            }

            remainingBalance = remainingBalance.subtract(principalPayment);
            if (remainingBalance.compareTo(BigDecimal.ZERO) < 0) {
                remainingBalance = BigDecimal.ZERO;
            }

            currentDate = dayCountCalculator.calculateDueDate(
                    request.getStartDate(), i, request.getPaymentFrequency()
            );

            if (request.isAdjustForBusinessDays()) {
                currentDate = dayCountCalculator.adjustToBusinessDay(currentDate);
            }

            int daysCount = dayCountCalculator.calculateDays(
                    request.getStartDate().plusMonths(i - 1),
                    currentDate,
                    request.getDayCountConvention()
            );

            schedule.add(AmortizationScheduleDTO.builder()
                    .installmentNumber(i)
                    .dueDate(currentDate)
                    .installmentAmount(fixedInstallment.setScale(4, RoundingMode.HALF_UP))
                    .principalAmount(principalPayment.setScale(4, RoundingMode.HALF_UP))
                    .interestAmount(interest.setScale(4, RoundingMode.HALF_UP))
                    .remainingBalance(remainingBalance.setScale(4, RoundingMode.HALF_UP))
                    .daysCount(daysCount)
                    .adjustedForBusinessDay(false)
                    .build());
        }

        log.info("✅ Calendario Francés generado: {} cuotas de {}", totalPeriods, fixedInstallment);
        return schedule;
    }

    private BigDecimal calculateFixedInstallment(BigDecimal principal, BigDecimal rate, int periods) {
        if (rate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(new BigDecimal(periods), MC);
        }

        // C = P * [i(1+i)^n] / [(1+i)^n - 1]
        double rateDouble = rate.doubleValue();
        double factor = Math.pow(1 + rateDouble, periods);
        double installment = principal.doubleValue() * (rateDouble * factor) / (factor - 1);

        return BigDecimal.valueOf(installment).setScale(4, RoundingMode.HALF_UP);
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
        return AmortizationMethod.FRENCH;
    }
}