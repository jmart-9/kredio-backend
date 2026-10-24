package com.kredio.backend.config;

import com.kredio.backend.entity.Loan;
import com.kredio.backend.entity.LoanSchedule;
import com.kredio.backend.repository.LoanRepository;
import com.kredio.backend.repository.LoanScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class ScheduledTaskConfig {

    private final LoanScheduleRepository scheduleRepository;
    private final LoanRepository loanRepository;

    /**
     * Se ejecuta cada hora para actualizar cuotas vencidas y calcular mora
     * Cron: 0 0 * * * * (cada hora en punto)
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void updateOverdueSchedules() {
        log.info("🔄 Ejecutando tarea programada: Actualización de morosidad y cálculo de mora");
        LocalDate today = LocalDate.now();
        int updatedCount = 0;
        int moratoryCalculated = 0;

        // Buscar TODAS las cuotas que hayan vencido (PENDING u OVERDUE)
        List<LoanSchedule> overdueSchedules = scheduleRepository.findAll().stream()
                .filter(schedule ->
                        (schedule.getStatus() == LoanSchedule.ScheduleStatus.PENDING ||
                                schedule.getStatus() == LoanSchedule.ScheduleStatus.OVERDUE) &&
                                schedule.getDueDate().isBefore(today)
                )
                .toList();

        log.info("📊 Encontradas {} cuotas vencidas para procesar", overdueSchedules.size());

        for (LoanSchedule schedule : overdueSchedules) {
            // Marcar como OVERDUE si estaba PENDING
            if (schedule.getStatus() == LoanSchedule.ScheduleStatus.PENDING) {
                schedule.setStatus(LoanSchedule.ScheduleStatus.OVERDUE);
            }

            // ✅ CALCULAR MORA ACUMULADA
            BigDecimal moratoryInterest = calculateMoratoryInterest(schedule, today);
            schedule.setMoratoryInterest(moratoryInterest);

            if (moratoryInterest.compareTo(BigDecimal.ZERO) > 0) {
                moratoryCalculated++;
                log.info("📊 Cuota #{} (Loan: {}) - Mora calculada: ${} ({} días de retraso)",
                        schedule.getInstallmentNumber(),
                        schedule.getLoanId(),
                        moratoryInterest,
                        ChronoUnit.DAYS.between(schedule.getDueDate(), today));
            }

            scheduleRepository.save(schedule);
            updatedCount++;

            // Marcar el préstamo como DEFAULTED
            Loan loan = loanRepository.findById(schedule.getLoanId()).orElse(null);
            if (loan != null && loan.getStatus() == Loan.LoanStatus.ACTIVE) {
                loan.setStatus(Loan.LoanStatus.DEFAULTED);
                loanRepository.save(loan);
                log.info("⚠️ Préstamo {} marcado como DEFAULTED", loan.getId());
            }
        }

        log.info("✅ Tarea completada: {} cuotas procesadas, {} con mora calculada", updatedCount, moratoryCalculated);
    }

    /**
     * Calcula el interés moratorio acumulado hasta la fecha actual
     * Fórmula: Saldo Pendiente × Tasa Moratoria Diaria × Días de Retraso
     */
    private BigDecimal calculateMoratoryInterest(LoanSchedule schedule, LocalDate today) {
        // Calcular días de retraso
        long daysLate = ChronoUnit.DAYS.between(schedule.getDueDate(), today);

        if (daysLate <= 0) {
            return BigDecimal.ZERO;
        }

        // Obtener el préstamo para acceder a la tasa moratoria y días de gracia
        Loan loan = loanRepository.findById(schedule.getLoanId()).orElse(null);
        if (loan == null) {
            log.warn("⚠️ No se encontró el préstamo para la cuota {}", schedule.getId());
            return BigDecimal.ZERO;
        }

        // Respetar días de gracia
        int graceDays = loan.getGraceDays() != null ? loan.getGraceDays() : 0;
        long effectiveDaysLate = Math.max(0, daysLate - graceDays);

        if (effectiveDaysLate == 0) {
            log.debug("Cuota #{} tiene {} días de gracia, no genera mora",
                    schedule.getInstallmentNumber(), graceDays);
            return BigDecimal.ZERO;
        }

        // Calcular saldo pendiente de la cuota
        BigDecimal expectedAmount = schedule.getExpectedAmount();
        BigDecimal paidAmount = schedule.getPaidAmount() != null ? schedule.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal pendingAmount = expectedAmount.subtract(paidAmount);

        if (pendingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Obtener tasa moratoria diaria del préstamo
        BigDecimal moratoryRateDaily = loan.getMoratoryRateDaily() != null
                ? loan.getMoratoryRateDaily()
                : new BigDecimal("0.001"); // 0.1% diario por defecto

        // Fórmula: Saldo Pendiente × Tasa Moratoria Diaria × Días de Retraso
        BigDecimal moratoryInterest = pendingAmount
                .multiply(moratoryRateDaily)
                .multiply(BigDecimal.valueOf(effectiveDaysLate))
                .setScale(4, RoundingMode.HALF_UP);

        log.debug("📊 Cuota #{}: Días retraso={}, Saldo=${}, Tasa={}, Mora=${}",
                schedule.getInstallmentNumber(), effectiveDaysLate, pendingAmount, moratoryRateDaily, moratoryInterest);

        return moratoryInterest;
    }

    /**
     * Se ejecuta al iniciar la aplicación (5 segundos después del startup)
     */
    @Scheduled(initialDelay = 5000, fixedDelay = Long.MAX_VALUE)
    @Transactional
    public void updateOverdueOnStartup() {
        log.info("🚀 Ejecutando sincronización de morosidad al iniciar la aplicación");
        updateOverdueSchedules();
    }
}