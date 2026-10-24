package com.kredio.backend.service;

import com.kredio.backend.dto.PaymentRequest;
import com.kredio.backend.dto.PaymentResponse;
import com.kredio.backend.entity.Loan;
import com.kredio.backend.entity.LoanSchedule;
import com.kredio.backend.entity.Payment;
import com.kredio.backend.engine.EarlyRepaymentPolicy;
import com.kredio.backend.repository.LoanRepository;
import com.kredio.backend.repository.LoanScheduleRepository;
import com.kredio.backend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.kredio.backend.engine.EarlyRepaymentPolicy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final LoanRepository loanRepository;
    private final LoanScheduleRepository scheduleRepository;
    private final ScheduleRecalculationService recalculationService;

    // ✅ NUEVAS INYECCIONES
    private final PaymentLedgerService paymentLedgerService;
    private final PinValidationService pinValidationService;

    @Transactional
    public PaymentResponse registerPayment(UUID tenantId, PaymentRequest request, UUID collectorId) {
        log.info("📝 Registrando pago: loanId={}, amount={}, collectorId={}, scheduleId={}",
                request.loanId(), request.amountPaid(), collectorId, request.scheduleId());

        Loan loan = loanRepository.findById(request.loanId())
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));

        if (!loan.getTenantId().equals(tenantId)) {
            throw new RuntimeException("El préstamo no pertenece a este tenant");
        }

        if (loan.getStatus() != Loan.LoanStatus.ACTIVE &&
                loan.getStatus() != Loan.LoanStatus.DEFAULTED &&
                loan.getStatus() != Loan.LoanStatus.PAID) {
            throw new RuntimeException("El préstamo no permite pagos. Estado actual: " + loan.getStatus());
        }

        BigDecimal amountPaid = request.amountPaid();
        if (amountPaid == null || amountPaid.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El monto pagado debe ser mayor a cero");
        }

        List<LoanSchedule> schedules = scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loan.getId());
        BigDecimal remainingAmount = amountPaid.setScale(4, RoundingMode.HALF_UP);

        BigDecimal totalCapitalApplied = BigDecimal.ZERO;
        BigDecimal totalInterestApplied = BigDecimal.ZERO;
        BigDecimal totalMoratoryApplied = BigDecimal.ZERO;
        BigDecimal excessApplied = BigDecimal.ZERO;

        // ✅ CORRECTO - Usar el enum del engine
        boolean hasCondonation = loan.getEarlyRepaymentPolicy() != null &&
                loan.getEarlyRepaymentPolicy() == com.kredio.backend.engine.EarlyRepaymentPolicy.WITH_CONDONATION;

        log.info("🔍 Política de condonación: {}, hasCondonation: {}", loan.getEarlyRepaymentPolicy(), hasCondonation);

        // ==========================================
        // 1. WATERFALL: Aplicar pago a la cuota específica
        // ==========================================
        for (LoanSchedule schedule : schedules) {
            if (request.scheduleId() != null && !schedule.getId().equals(request.scheduleId())) {
                continue;
            }

            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) break;
            if (schedule.getStatus() == LoanSchedule.ScheduleStatus.PAID) continue;

            BigDecimal expectedAmount = schedule.getAdjustedAmount() != null
                    ? schedule.getAdjustedAmount()
                    : schedule.getExpectedAmount();
            BigDecimal expectedPrincipal = schedule.getAdjustedPrincipal() != null
                    ? schedule.getAdjustedPrincipal()
                    : schedule.getExpectedPrincipal();
            BigDecimal expectedInterest = schedule.getAdjustedInterest() != null
                    ? schedule.getAdjustedInterest()
                    : schedule.getExpectedInterest();

            BigDecimal moratoryInterest = schedule.getMoratoryInterest() != null ?
                    schedule.getMoratoryInterest().setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            BigDecimal paidAmount = schedule.getPaidAmount() != null ?
                    schedule.getPaidAmount().setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            BigDecimal pendingAmount = expectedAmount.subtract(paidAmount).add(moratoryInterest);
            if (pendingAmount.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal appliedToThisSchedule = BigDecimal.ZERO;

            // a. Mora (1er lugar)
            if (moratoryInterest.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal toApply = remainingAmount.min(moratoryInterest);
                totalMoratoryApplied = totalMoratoryApplied.add(toApply);
                remainingAmount = remainingAmount.subtract(toApply);
                appliedToThisSchedule = appliedToThisSchedule.add(toApply);
                schedule.setMoratoryInterest(moratoryInterest.subtract(toApply));
                log.info("   → Aplicado ${} a mora", toApply);
            }

            // b. Interés de la cuota (2do lugar)
            if (expectedInterest.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal toApply = remainingAmount.min(expectedInterest);
                totalInterestApplied = totalInterestApplied.add(toApply);
                remainingAmount = remainingAmount.subtract(toApply);
                appliedToThisSchedule = appliedToThisSchedule.add(toApply);
                log.info("   → Aplicado ${} a interés", toApply);
            }

            // c. Capital de la cuota (3er lugar)
            if (expectedPrincipal.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal toApply = remainingAmount.min(expectedPrincipal);
                totalCapitalApplied = totalCapitalApplied.add(toApply);
                remainingAmount = remainingAmount.subtract(toApply);
                appliedToThisSchedule = appliedToThisSchedule.add(toApply);
                log.info("   → Aplicado ${} a capital", toApply);
            }

            // Actualizar estado de la cuota
            BigDecimal newPaidAmount = paidAmount.add(appliedToThisSchedule).setScale(4, RoundingMode.HALF_UP);
            schedule.setPaidAmount(newPaidAmount);

            if (newPaidAmount.compareTo(expectedAmount.add(moratoryInterest)) >= 0) {
                schedule.setStatus(LoanSchedule.ScheduleStatus.PAID);
                schedule.setPaidDate(Instant.now());
                log.info("💰 Cuota #{} - Pagada completamente", schedule.getInstallmentNumber());
            } else {
                schedule.setStatus(LoanSchedule.ScheduleStatus.PARTIAL);
                log.info("💰 Cuota #{} - Pago parcial: {}", schedule.getInstallmentNumber(), newPaidAmount);
            }
            scheduleRepository.save(schedule);
        }

        // ==========================================
        // 2. MANEJO DEL EXCEDENTE CON RECALCULACIÓN
        // ==========================================
        if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
            excessApplied = remainingAmount;
            if (hasCondonation) {
                log.info("💡 Condonación ACTIVA: Aplicando excedente de ${} a capital de cuotas futuras", excessApplied);
                totalCapitalApplied = totalCapitalApplied.add(excessApplied);

                List<LoanSchedule> modifiedSchedules = recalculationService.applyExcessToFutureSchedules(
                        loan.getId(),
                        excessApplied,
                        collectorId
                );
                log.info("✅ Se modificaron {} cuotas futuras", modifiedSchedules.size());
            } else {
                log.info("💡 Condonación INACTIVA: Excedente de ${} se aplicará a INTERÉS", excessApplied);
                totalInterestApplied = totalInterestApplied.add(excessApplied);
            }
            remainingAmount = BigDecimal.ZERO;
        }

        log.info("✅ Pago registrado - Total: ${}, Mora: ${}, Interés: ${}, Capital: ${}, Excedente: ${}",
                amountPaid, totalMoratoryApplied, totalInterestApplied, totalCapitalApplied, excessApplied);

        // ==========================================
        // 3. Guardar el registro del pago
        // ==========================================
        Payment payment = Payment.builder()
                .tenantId(tenantId)
                .loanId(request.loanId())
                .scheduleId(request.scheduleId())
                .collectorId(collectorId)
                .amountPaid(amountPaid)
                .appliedToMoratory(totalMoratoryApplied)
                .appliedToInterest(totalInterestApplied)
                .appliedToPrincipal(totalCapitalApplied)
                .excessAmount(excessApplied)
                .method(request.method() != null ? Payment.PaymentMethod.valueOf(request.method()) : Payment.PaymentMethod.CASH)
                .isPrinted(request.isPrinted() != null ? request.isPrinted() : false)
                .syncedFromOffline(request.syncedFromOffline() != null ? request.syncedFromOffline() : false)
                .paymentDate(Instant.now())
                .isVoided(false)
                .build();

        paymentRepository.save(payment);

        // ==========================================
        // 4. Actualizar saldo del préstamo
        // ==========================================
        BigDecimal previousBalance = loan.getCurrentBalance();
        BigDecimal newBalance = previousBalance.subtract(totalCapitalApplied).setScale(4, RoundingMode.HALF_UP);

        if (newBalance.compareTo(BigDecimal.ZERO) <= 0) {
            loan.setStatus(Loan.LoanStatus.PAID);
            loan.setCurrentBalance(BigDecimal.ZERO);
            log.info("✅ Préstamo liquidado completamente");
        } else {
            loan.setCurrentBalance(newBalance);
        }
        loanRepository.save(loan);

        // ==========================================
        // 5. REGISTRO EN EL LIBRO CONTABLE (LEDGER)
        // ==========================================
        if (request.scheduleId() != null) {
            paymentLedgerService.recordEntry(
                    tenantId,
                    payment.getId(),
                    request.scheduleId(),
                    "PAYMENT",
                    totalMoratoryApplied,
                    totalInterestApplied,
                    totalCapitalApplied,
                    amountPaid,
                    previousBalance,
                    loan.getCurrentBalance(),
                    collectorId,
                    "Pago registrado - Método: " + payment.getMethod().name()
            );
        }

        return new PaymentResponse(
                payment.getId(), payment.getLoanId(), payment.getCollectorId(), payment.getAmountPaid(),
                previousBalance, loan.getCurrentBalance(), payment.getPaymentDate(),
                payment.getMethod().name(), payment.getIsPrinted(), payment.getSyncedFromOffline()
        );
    }

    public List<Payment> getPaymentsByLoan(UUID tenantId, UUID loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));
        if (!loan.getTenantId().equals(tenantId)) {
            throw new RuntimeException("El préstamo no pertenece a este tenant");
        }
        return paymentRepository.findByLoanIdOrderByPaymentDateDesc(loanId);
    }

    /**
     * Anula un pago con validación de PIN de supervisor/admin
     * Reemplaza al antiguo voidPayment
     */
    @Transactional
    public void annulPayment(UUID tenantId, UUID paymentId, UUID annulledByUserId, String annulmentReason, String supervisorPin) {
        log.info("🔄 Anulando pago: paymentId={}, annulledBy={}, reason={}", paymentId, annulledByUserId, annulmentReason);

        // 1. Validar PIN del supervisor/admin
        pinValidationService.validatePin(annulledByUserId, supervisorPin);

        // 2. Obtener el pago
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado"));

        if (!payment.getTenantId().equals(tenantId)) {
            throw new RuntimeException("El pago no pertenece a este tenant");
        }

        if (Boolean.TRUE.equals(payment.getIsVoided())) {
            throw new RuntimeException("El pago ya fue anulado previamente");
        }

        // 3. Revertir el pago en las cuotas (waterfall inverso)
        revertPaymentFromSchedules(payment);

        // 4. Revertir el saldo del préstamo
        Loan loan = loanRepository.findById(payment.getLoanId())
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));

        BigDecimal capitalToRevert = payment.getAppliedToPrincipal() != null ?
                payment.getAppliedToPrincipal() : BigDecimal.ZERO;

        BigDecimal previousBalance = loan.getCurrentBalance();
        BigDecimal newBalance = previousBalance.add(capitalToRevert);
        loan.setCurrentBalance(newBalance);

        if (loan.getStatus() == Loan.LoanStatus.PAID) {
            loan.setStatus(Loan.LoanStatus.ACTIVE);
        }
        loanRepository.save(loan);

        // 5. Marcar el pago como anulado
        payment.setIsVoided(true);
        payment.setVoidedAt(Instant.now());
        payment.setAnnulmentReason(annulmentReason);
        payment.setAnnulledByUserId(annulledByUserId);
        payment.setAnnulledAt(Instant.now());
        paymentRepository.save(payment);

        // 6. REGISTRO EN EL LEDGER (REVERSAL)
        if (payment.getScheduleId() != null) {
            paymentLedgerService.recordReversal(
                    tenantId,
                    payment.getId(),
                    payment.getScheduleId(),
                    payment.getAppliedToMoratory() != null ? payment.getAppliedToMoratory() : BigDecimal.ZERO,
                    payment.getAppliedToInterest() != null ? payment.getAppliedToInterest() : BigDecimal.ZERO,
                    payment.getAppliedToPrincipal() != null ? payment.getAppliedToPrincipal() : BigDecimal.ZERO,
                    payment.getAmountPaid(),
                    previousBalance,
                    newBalance,
                    annulledByUserId,
                    annulmentReason
            );
        }

        log.info("✅ Pago anulado exitosamente. Saldo restaurado: ${} -> ${}", previousBalance, newBalance);
    }

    private void revertPaymentFromSchedules(Payment payment) {
        List<LoanSchedule> schedules = scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(payment.getLoanId());
        BigDecimal amountToRevert = payment.getAmountPaid();

        for (LoanSchedule schedule : schedules) {
            if (amountToRevert.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal paid = schedule.getPaidAmount() != null ? schedule.getPaidAmount() : BigDecimal.ZERO;
            if (paid.compareTo(BigDecimal.ZERO) > 0) {
                if (amountToRevert.compareTo(paid) >= 0) {
                    schedule.setPaidAmount(BigDecimal.ZERO);
                    schedule.setStatus(LoanSchedule.ScheduleStatus.PENDING);
                    schedule.setPaidDate(null);
                    amountToRevert = amountToRevert.subtract(paid);
                } else {
                    schedule.setPaidAmount(paid.subtract(amountToRevert).setScale(4, RoundingMode.HALF_UP));
                    schedule.setStatus(LoanSchedule.ScheduleStatus.PARTIAL);
                    amountToRevert = BigDecimal.ZERO;
                }
                scheduleRepository.save(schedule);
            }
        }
    }
}