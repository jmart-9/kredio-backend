package com.kredio.backend.config;

import com.kredio.backend.repository.LoanRepository;
import com.kredio.backend.repository.LoanScheduleRepository;
import com.kredio.backend.entity.Loan;
import com.kredio.backend.entity.LoanSchedule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class ScheduledTaskConfig {

    private final LoanScheduleRepository scheduleRepository;
    private final LoanRepository loanRepository;

    /**
     * Se ejecuta cada hora para actualizar cuotas vencidas
     * Cron: 0 0 * * * * (cada hora en punto)
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void updateOverdueSchedules() {
        log.info("🔄 Ejecutando tarea programada: Actualización de morosidad");

        LocalDate today = LocalDate.now();
        int updatedCount = 0;

        // Buscar todas las cuotas PENDING con fecha de vencimiento anterior a hoy
        List<LoanSchedule> overdueSchedules = scheduleRepository.findAll().stream()
                .filter(schedule ->
                        schedule.getStatus() == LoanSchedule.ScheduleStatus.PENDING &&
                                schedule.getDueDate().isBefore(today)
                )
                .toList();

        // Marcar cada cuota como OVERDUE
        for (LoanSchedule schedule : overdueSchedules) {
            schedule.setStatus(LoanSchedule.ScheduleStatus.OVERDUE);
            scheduleRepository.save(schedule);
            updatedCount++;

            // Marcar el préstamo como DEFAULTED
            Loan loan = loanRepository.findById(schedule.getLoanId()).orElse(null);
            if (loan != null && loan.getStatus() == Loan.LoanStatus.ACTIVE) {
                loan.setStatus(Loan.LoanStatus.DEFAULTED);
                loanRepository.save(loan);
            }
        }

        if (updatedCount > 0) {
            log.info("✅ Tarea programada completada: {} cuotas marcadas como OVERDUE", updatedCount);
        } else {
            log.info("ℹ️ Tarea programada: No hay cuotas vencidas");
        }
    }

    /**
     * Se ejecuta al iniciar la aplicación
     */
    @Scheduled(initialDelay = 5000, fixedDelay = Long.MAX_VALUE)
    @Transactional
    public void updateOverdueOnStartup() {
        log.info("🚀 Ejecutando actualización de morosidad al iniciar la aplicación");
        updateOverdueSchedules();
    }
}