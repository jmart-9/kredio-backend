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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final LoanRepository loanRepository;
    private final LoanScheduleRepository scheduleRepository;

    @Transactional
    public PaymentResponse registerPayment(PaymentRequest request, UUID tenantId) {
        // 1. Buscar el préstamo
        Loan loan = loanRepository.findById(request.loanId())
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));

        // Validar que el préstamo pertenezca al tenant
        if (!loan.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Acceso denegado: el préstamo no pertenece a esta financiera");
        }

        // Validar que el préstamo esté activo
        if (loan.getStatus() != Loan.LoanStatus.ACTIVE) {
            throw new RuntimeException("El préstamo no está activo. Estado actual: " + loan.getStatus());
        }

        BigDecimal previousBalance = loan.getCurrentBalance();
        BigDecimal amountPaid = request.amountPaid();

        // 2. Validar que el monto no sea mayor al saldo
        if (amountPaid.compareTo(previousBalance) > 0) {
            throw new RuntimeException("El monto pagado (" + amountPaid + ") es mayor al saldo pendiente (" + previousBalance + ")");
        }

        // 3. Crear el registro de pago
        Payment payment = Payment.builder()
                .tenantId(tenantId)
                .loanId(request.loanId())
                .collectorId(request.collectorId())
                .amountPaid(amountPaid)
                .method(Payment.PaymentMethod.valueOf(request.method()))
                .isPrinted(request.isPrinted() != null ? request.isPrinted() : false)
                .syncedFromOffline(request.syncedFromOffline() != null ? request.syncedFromOffline() : false)
                .build();

        payment = paymentRepository.save(payment);

        // 4. Actualizar el saldo del préstamo
        BigDecimal newBalance = previousBalance.subtract(amountPaid).setScale(4, RoundingMode.HALF_UP);
        loan.setCurrentBalance(newBalance);

        // Si el saldo llega a 0, marcar el préstamo como PAGADO
        if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
            loan.setStatus(Loan.LoanStatus.PAID);
        }

        loanRepository.save(loan);

        // 5. Actualizar las cuotas afectadas
        applyPaymentToSchedules(loan, amountPaid);

        // 6. Retornar respuesta
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
     * Aplica el pago a las cuotas pendientes (de la más antigua a la más reciente)
     */
    private void applyPaymentToSchedules(Loan loan, BigDecimal amountPaid) {
        // Obtener todas las cuotas pendientes del préstamo, ordenadas por fecha
        List<LoanSchedule> pendingSchedules = scheduleRepository
                .findByLoanIdAndStatusOrderByDueDateAsc(loan.getId(), LoanSchedule.ScheduleStatus.PENDING);

        BigDecimal remainingPayment = amountPaid;

        for (LoanSchedule schedule : pendingSchedules) {
            if (remainingPayment.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal expectedAmount = schedule.getExpectedAmount();

            // Si el pago cubre toda la cuota
            if (remainingPayment.compareTo(expectedAmount) >= 0) {
                schedule.setStatus(LoanSchedule.ScheduleStatus.PAID);
                remainingPayment = remainingPayment.subtract(expectedAmount);
            } else {
                // El pago es parcial (no se marca como pagada aún)
                // En un sistema más avanzado, aquí calcularías cuánto se pagó de capital vs interés
                break;
            }

            scheduleRepository.save(schedule);
        }
    }
}