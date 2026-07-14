package com.kredio.backend.service;

import com.kredio.backend.entity.LoanSchedule;
import com.kredio.backend.repository.LoanScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final LoanScheduleRepository scheduleRepository;

    /**
     * Corre cada día a las 2 AM para marcar cuotas vencidas
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void markOverdueSchedules() {
        LocalDate today = LocalDate.now();
        List<LoanSchedule> pendingSchedules = scheduleRepository.findAll()
                .stream()
                .filter(s -> s.getStatus() == LoanSchedule.ScheduleStatus.PENDING)
                .filter(s -> s.getDueDate().isBefore(today))
                .toList();

        for (LoanSchedule schedule : pendingSchedules) {
            schedule.setStatus(LoanSchedule.ScheduleStatus.OVERDUE);
            scheduleRepository.save(schedule);
        }

        log.info("Marcadas {} cuotas como vencidas", pendingSchedules.size());
    }
}