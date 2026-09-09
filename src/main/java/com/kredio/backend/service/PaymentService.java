package com.kredio.backend.service;

import com.kredio.backend.dto.PaymentRequest;
import com.kredio.backend.dto.PaymentResponse;
import com.kredio.backend.entity.Loan;
import com.kredio.backend.entity.LoanSchedule;
import com.kredio.backend.entity.Payment;
import com.kredio.backend.repository.LoanRepository;
import com.kredio.backend.repository.LoanScheduleRepository;
import com.kredio.backend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final LoanRepository loanRepository;
    private final LoanScheduleRepository scheduleRepository;

    /**
     * Registrar un nuevo pago
     */
    @Transactional
    public PaymentResponse registerPayment(UUID tenantId, PaymentRequest request) {
        // 1. Validar que el préstamo existe y pertenece al tenant
        Loan loan = loanRepository.findById(request.loanId())
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));

        if (!loan.getTenantId().equals(tenantId)) {
            throw new RuntimeException("El préstamo no pertenece a este tenant");
        }

        // 2. Validar que el préstamo esté activo
        if (loan.getStatus() != Loan.LoanStatus.ACTIVE) {
            throw new RuntimeException("El préstamo no está activo. Estado actual: " + loan.getStatus());
        }

        // 3. Validar que el monto no exceda el saldo actual
        BigDecimal amountPaid = request.amountPaid();
        BigDecimal previousBalance = loan.getCurrentBalance();

        if (amountPaid.compareTo(previousBalance) > 0) {
            throw new RuntimeException("El monto pagado excede el saldo actual del préstamo");
        }

        // 4. Crear el registro de pago
        Payment payment = Payment.builder()
                .tenantId(tenantId)
                .loanId(request.loanId())
                .collectorId(request.collectorId())
                .amountPaid(amountPaid)
                .method(Payment.PaymentMethod.valueOf(request.method()))
                .isPrinted(request.isPrinted() != null ? request.isPrinted() : false)
                .syncedFromOffline(request.syncedFromOffline() != null ? request.syncedFromOffline() : false)
                .paymentDate(Instant.now())
                .isVoided(false)
                .build();

        payment = paymentRepository.save(payment);

        // 5. Aplicar el pago a las cuotas pendientes
        applyPaymentToSchedules(loan, amountPaid);

        // 6. Actualizar el saldo del préstamo
        BigDecimal newBalance = previousBalance.subtract(amountPaid);
        loan.setCurrentBalance(newBalance);

        // Si el saldo es cero, marcar el préstamo como pagado
        if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
            loan.setStatus(Loan.LoanStatus.PAID);
        }

        loanRepository.save(loan);

        // 7. Retornar respuesta
        return new PaymentResponse(
                payment.getId(),
                payment.getLoanId(),
                payment.getCollectorId(),
                payment.getAmountPaid(),
                previousBalance,
                newBalance,
                payment.getPaymentDate(),
                payment.getMethod().name(),
                payment.getIsPrinted(),
                payment.getSyncedFromOffline()
        );
    }

    /**
     * Aplicar pago a las cuotas pendientes (de más antigua a más reciente)
     */
    private void applyPaymentToSchedules(Loan loan, BigDecimal amountPaid) {
        // Obtener cuotas pendientes ordenadas por número de cuota
        List<LoanSchedule> pendingSchedules = scheduleRepository
                .findByLoanIdAndStatusOrderByDueDateAsc(loan.getId(), LoanSchedule.ScheduleStatus.PENDING);

        BigDecimal remainingAmount = amountPaid;

        for (LoanSchedule schedule : pendingSchedules) {
            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal expectedAmount = schedule.getExpectedAmount();
            BigDecimal paidAmount = schedule.getPaidAmount() != null ? schedule.getPaidAmount() : BigDecimal.ZERO;
            BigDecimal pendingAmount = expectedAmount.subtract(paidAmount);

            if (remainingAmount.compareTo(pendingAmount) >= 0) {
                // Pago completo de esta cuota
                schedule.setPaidAmount(expectedAmount);
                schedule.setStatus(LoanSchedule.ScheduleStatus.PAID);
                schedule.setPaidDate(Instant.now());
                remainingAmount = remainingAmount.subtract(pendingAmount);
            } else {
                // Pago parcial
                schedule.setPaidAmount(paidAmount.add(remainingAmount));
                schedule.setStatus(LoanSchedule.ScheduleStatus.PARTIAL);
                schedule.setPaidDate(Instant.now());
                remainingAmount = BigDecimal.ZERO;
            }

            scheduleRepository.save(schedule);
        }
    }

    /**
     * Anular un pago (void)
     */
    @Transactional
    public void voidPayment(UUID tenantId, UUID paymentId) {
        // 1. Buscar el pago
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado"));

        if (!payment.getTenantId().equals(tenantId)) {
            throw new RuntimeException("El pago no pertenece a este tenant");
        }

        // 2. Validar que no esté anulado
        if (payment.getIsVoided() != null && payment.getIsVoided()) {
            throw new RuntimeException("El pago ya fue anulado");
        }

        // 3. Revertir el pago en las cuotas
        revertPaymentFromSchedules(payment);

        // 4. Actualizar el saldo del préstamo
        Loan loan = loanRepository.findById(payment.getLoanId())
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));

        BigDecimal newBalance = loan.getCurrentBalance().add(payment.getAmountPaid());
        loan.setCurrentBalance(newBalance);
        loan.setStatus(Loan.LoanStatus.ACTIVE);
        loanRepository.save(loan);

        // 5. Marcar el pago como anulado
        payment.setIsVoided(true);
        payment.setVoidedAt(Instant.now());
        paymentRepository.save(payment);
    }

    /**
     * Revertir el pago de las cuotas
     */
    private void revertPaymentFromSchedules(Payment payment) {
        List<LoanSchedule> schedules = scheduleRepository.findByLoanId(payment.getLoanId());
        BigDecimal remainingAmount = payment.getAmountPaid();

        // Revertir de la más reciente a la más antigua
        for (int i = schedules.size() - 1; i >= 0 && remainingAmount.compareTo(BigDecimal.ZERO) > 0; i--) {
            LoanSchedule schedule = schedules.get(i);

            if (schedule.getStatus() == LoanSchedule.ScheduleStatus.PAID) {
                BigDecimal expectedAmount = schedule.getExpectedAmount();

                if (remainingAmount.compareTo(expectedAmount) >= 0) {
                    // Revertir completamente
                    schedule.setStatus(LoanSchedule.ScheduleStatus.PENDING);
                    schedule.setPaidAmount(BigDecimal.ZERO);
                    schedule.setPaidDate(null);
                    remainingAmount = remainingAmount.subtract(expectedAmount);
                } else {
                    // Revertir parcialmente
                    schedule.setStatus(LoanSchedule.ScheduleStatus.PARTIAL);
                    schedule.setPaidAmount(expectedAmount.subtract(remainingAmount));
                    remainingAmount = BigDecimal.ZERO;
                }

                scheduleRepository.save(schedule);
            }
        }
    }

    /**
     * Obtener historial de pagos de un préstamo
     */
    public List<PaymentResponse> getPaymentsByLoan(UUID tenantId, UUID loanId) {
        List<Payment> payments = paymentRepository.findByLoanId(loanId);

        return payments.stream()
                .map(p -> new PaymentResponse(
                        p.getId(),
                        p.getLoanId(),
                        p.getCollectorId(),
                        p.getAmountPaid(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        p.getPaymentDate(),
                        p.getMethod().name(),
                        p.getIsPrinted(),
                        p.getSyncedFromOffline()
                ))
                .toList();
    }
}