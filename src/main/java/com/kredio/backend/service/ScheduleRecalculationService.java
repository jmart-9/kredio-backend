package com.kredio.backend.service;

import com.kredio.backend.entity.LoanSchedule;
import com.kredio.backend.repository.LoanScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleRecalculationService {

    private final LoanScheduleRepository scheduleRepository;

    /**
     * Recalcula las cuotas futuras aplicando el excedente SOLO al capital.
     * El interés se mantiene vigente y debe ser pagado por el cliente.
     *
     * @param loanId ID del préstamo
     * @param excessAmount Monto excedente a aplicar
     * @param condonatedBy ID del usuario que autorizó la condonación
     * @return Lista de cuotas modificadas
     */
    @Transactional
    public List<LoanSchedule> applyExcessToFutureSchedules(
            UUID loanId,
            BigDecimal excessAmount,
            UUID condonatedBy
    ) {
        log.info("🔄 Recalculando cuotas futuras para préstamo {} con excedente de ${}", loanId, excessAmount);

        List<LoanSchedule> futureSchedules = scheduleRepository
                .findByLoanIdOrderByInstallmentNumberAsc(loanId)
                .stream()
                .filter(s -> s.getStatus() != LoanSchedule.ScheduleStatus.PAID)
                .toList();

        BigDecimal remainingExcess = excessAmount.setScale(4, RoundingMode.HALF_UP);
        int modifiedCount = 0;

        for (LoanSchedule schedule : futureSchedules) {
            if (remainingExcess.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal currentPrincipal = schedule.getAdjustedPrincipal() != null
                    ? schedule.getAdjustedPrincipal()
                    : schedule.getExpectedPrincipal();

            BigDecimal currentInterest = schedule.getAdjustedInterest() != null
                    ? schedule.getAdjustedInterest()
                    : schedule.getExpectedInterest();

            // ✅ CORRECCIÓN: Solo aplicar al CAPITAL, NO condonar interés
            if (remainingExcess.compareTo(currentPrincipal) >= 0) {
                // El excedente cubre todo el capital de esta cuota
                BigDecimal condonatedPrincipal = currentPrincipal;

                // ✅ Mantener el interés intacto
                schedule.setAdjustedPrincipal(BigDecimal.ZERO);
                schedule.setAdjustedInterest(currentInterest); // ✅ NO se condona
                schedule.setAdjustedAmount(currentInterest); // ✅ Solo el interés queda pendiente
                schedule.setCondonatedAmount(condonatedPrincipal);
                schedule.setCondonatedInterest(BigDecimal.ZERO); // ✅ NO se condona interés
                schedule.setCondonatedAt(Instant.now());
                schedule.setCondonatedBy(condonatedBy);

                // ✅ NO cambiar estado a CONDONATED, mantener PENDING
                // El cliente aún debe pagar el interés

                remainingExcess = remainingExcess.subtract(condonatedPrincipal);
                modifiedCount++;

                log.info("✅ Cuota #{} - Capital condonado: ${}, Interés pendiente: ${}",
                        schedule.getInstallmentNumber(), condonatedPrincipal, currentInterest);

            } else {
                // Condonación parcial del capital
                BigDecimal condonatedPrincipal = remainingExcess;
                BigDecimal newPrincipal = currentPrincipal.subtract(condonatedPrincipal);

                schedule.setAdjustedPrincipal(newPrincipal);
                schedule.setAdjustedInterest(currentInterest); // ✅ Mantener interés
                schedule.setAdjustedAmount(newPrincipal.add(currentInterest));
                schedule.setCondonatedAmount(condonatedPrincipal);
                schedule.setCondonatedInterest(BigDecimal.ZERO); // ✅ NO se condona interés
                schedule.setCondonatedAt(Instant.now());
                schedule.setCondonatedBy(condonatedBy);

                remainingExcess = BigDecimal.ZERO;

                log.info("⚡ Cuota #{} - Capital reducido de ${} a ${}, Interés pendiente: ${}",
                        schedule.getInstallmentNumber(), currentPrincipal, newPrincipal, currentInterest);
            }

            scheduleRepository.save(schedule);
        }

        log.info(" Total de cuotas modificadas: {}", modifiedCount);
        return futureSchedules;
    }

    /**
     * Calcula el total de capital condonado de un préstamo
     */
    public BigDecimal getTotalCondonatedAmount(UUID loanId) {
        return scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId)
                .stream()
                .filter(s -> s.getCondonatedAmount() != null && s.getCondonatedAmount().compareTo(BigDecimal.ZERO) > 0)
                .map(LoanSchedule::getCondonatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calcula el total de interés condonado (debería ser 0 con la nueva lógica)
     */
    public BigDecimal getTotalCondonatedInterest(UUID loanId) {
        return scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId)
                .stream()
                .filter(s -> s.getCondonatedInterest() != null && s.getCondonatedInterest().compareTo(BigDecimal.ZERO) > 0)
                .map(LoanSchedule::getCondonatedInterest)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}