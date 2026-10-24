package com.kredio.backend.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Slf4j
@Component
public class RateConverter {

    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);

    /**
     * Convierte tasa nominal anual a tasa periódica
     * Fórmula: i_periodica = i_nominal / m
     */
    public BigDecimal nominalToPeriodicRate(BigDecimal annualNominalRate, PaymentFrequency frequency) {
        BigDecimal m = new BigDecimal(frequency.getPeriodsPerYear());
        return annualNominalRate.divide(m, MC).divide(new BigDecimal("100"), MC);
    }

    /**
     * Convierte tasa efectiva anual a tasa periódica
     * Fórmula: i_periodica = (1 + i_efectiva)^(1/m) - 1
     */
    public BigDecimal effectiveToPeriodicRate(BigDecimal annualEffectiveRate, PaymentFrequency frequency) {
        BigDecimal m = new BigDecimal(frequency.getPeriodsPerYear());
        BigDecimal onePlusRate = BigDecimal.ONE.add(annualEffectiveRate.divide(new BigDecimal("100"), MC));
        BigDecimal exponent = BigDecimal.ONE.divide(m, MC);

        // Usamos Math.pow para el cálculo de raíz
        double result = Math.pow(onePlusRate.doubleValue(), exponent.doubleValue()) - 1.0;
        return BigDecimal.valueOf(result).setScale(10, RoundingMode.HALF_UP);
    }

    /**
     * Convierte tasa nominal a efectiva anual
     * Fórmula: i_efectiva = (1 + i_nominal/m)^m - 1
     */
    public BigDecimal nominalToEffectiveRate(BigDecimal annualNominalRate, PaymentFrequency frequency) {
        BigDecimal m = new BigDecimal(frequency.getPeriodsPerYear());
        BigDecimal periodicRate = annualNominalRate.divide(new BigDecimal("100"), MC).divide(m, MC);
        BigDecimal onePlusPeriodic = BigDecimal.ONE.add(periodicRate);

        double result = Math.pow(onePlusPeriodic.doubleValue(), m.doubleValue()) - 1.0;
        return BigDecimal.valueOf(result).multiply(new BigDecimal("100")).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Obtiene la tasa periódica según el tipo de tasa
     */
    public BigDecimal getPeriodicRate(BigDecimal annualRate, RateType rateType, PaymentFrequency frequency) {
        if (rateType == RateType.NOMINAL) {
            return nominalToPeriodicRate(annualRate, frequency);
        } else {
            return effectiveToPeriodicRate(annualRate, frequency);
        }
    }
}