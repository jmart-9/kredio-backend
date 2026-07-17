package com.kredio.backend.service;

import com.kredio.backend.entity.Loan;
import com.kredio.backend.entity.LoanSchedule;
import com.kredio.backend.repository.LoanRepository;
import com.kredio.backend.repository.LoanScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OverdueService {

    private final LoanScheduleRepository scheduleRepository;
    private final LoanRepository loanRepository;

    /**
     * Actualiza todas las cuotas pendientes que ya vencieron a estado OVERDUE
     * y marca los préstamos relacionados como DEFAULTED
     */
    @Transactional
    public int updateOverdueStatus(UUID tenantId) {
        LocalDate today = LocalDate.now();
        int updatedCount = 0;

        // 1. Buscar todas las cuotas PENDING con fecha de vencimiento anterior a hoy
        List<LoanSchedule> overdueSchedules = scheduleRepository.findAll().stream()
                .filter(schedule ->
                        schedule.getTenantId().equals(tenantId) &&
                                schedule.getStatus() == LoanSchedule.ScheduleStatus.PENDING &&
                                schedule.getDueDate().isBefore(today)
                )
                .toList();

        // 2. Marcar cada cuota como OVERDUE
        for (LoanSchedule schedule : overdueSchedules) {
            schedule.setStatus(LoanSchedule.ScheduleStatus.OVERDUE);
            scheduleRepository.save(schedule);
            updatedCount++;

            // 3. Marcar el préstamo como DEFAULTED si tiene cuotas vencidas
            Loan loan = loanRepository.findById(schedule.getLoanId()).orElse(null);
            if (loan != null && loan.getStatus() == Loan.LoanStatus.ACTIVE) {
                loan.setStatus(Loan.LoanStatus.DEFAULTED);
                loanRepository.save(loan);
                log.info("Préstamo {} marcado como DEFAULTED", loan.getId());
            }
        }

        if (updatedCount > 0) {
            log.info("✅ Actualización de morosidad completada: {} cuotas marcadas como OVERDUE", updatedCount);
        } else {
            log.info("️ No hay cuotas vencidas para actualizar");
        }

        return updatedCount;
    }
}