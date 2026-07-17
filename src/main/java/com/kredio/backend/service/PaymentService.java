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

        // ✅ CORRECCIÓN: Si el monto pagado es mayor al saldo pendiente,
        // lo ajustamos al saldo restante (esto es normal en la última cuota por redondeos)
        if (amountPaid.compareTo(previousBalance) > 0) {
            amountPaid = previousBalance;
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

        // 4. ✅ CORRECCIÓN CRÍTICA: Actualizar el saldo del préstamo
        // Obtener el capital amortizado de la cuota que se está pagando
        List<LoanSchedule> pendingSchedules = scheduleRepository
                .findByLoanIdAndStatusOrderByDueDateAsc(loan.getId(), LoanSchedule.ScheduleStatus.PENDING);

        BigDecimal capitalAmortizado = BigDecimal.ZERO;
        if (!pendingSchedules.isEmpty()) {
            LoanSchedule currentSchedule = pendingSchedules.get(0);
            // El capital amortizado es expectedPrincipal, NO expectedAmount
            capitalAmortizado = currentSchedule.getExpectedPrincipal();
        }

        // Restar SOLO el capital del saldo pendiente
        BigDecimal newBalance = previousBalance.subtract(capitalAmortizado).setScale(4, RoundingMode.HALF_UP);
        loan.setCurrentBalance(newBalance);

        // Si el saldo llega a 0 (o muy cerca de 0), marcar el préstamo como PAGADO
        if (newBalance.compareTo(BigDecimal.ZERO) <= 0) {
            loan.setStatus(Loan.LoanStatus.PAID);
            loan.setCurrentBalance(BigDecimal.ZERO); // Asegurar que quede exactamente en 0
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
                loan.getCurrentBalance(), // Usar el balance actualizado del loan
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
        List<LoanSchedule> pendingSchedules = scheduleRepository
                .findByLoanIdAndStatusOrderByDueDateAsc(loan.getId(), LoanSchedule.ScheduleStatus.PENDING);

        BigDecimal remainingPayment = amountPaid;

        for (LoanSchedule schedule : pendingSchedules) {
            if (remainingPayment.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal expectedAmount = schedule.getExpectedAmount();

            // ✅ CORRECCIÓN: Marcar como pagada si cubre la cuota O si el préstamo ya fue marcado como PAGADO
            if (remainingPayment.compareTo(expectedAmount) >= 0 || loan.getStatus() == Loan.LoanStatus.PAID) {
                schedule.setStatus(LoanSchedule.ScheduleStatus.PAID);
                remainingPayment = remainingPayment.subtract(expectedAmount);
                scheduleRepository.save(schedule);
            } else {
                // Pago parcial (no debería ocurrir si el backend ajustó el monto, pero por seguridad)
                break;
            }
        }
    }
}