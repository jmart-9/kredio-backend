package com.kredio.backend.service;

import com.kredio.backend.dto.LoanRequest;
import com.kredio.backend.dto.LoanResponse;
import com.kredio.backend.dto.LoanScheduleResponse;
import com.kredio.backend.engine.*;
import com.kredio.backend.engine.amortization.AmortizationFactory;
import com.kredio.backend.entity.Client;
import com.kredio.backend.entity.Loan;
import com.kredio.backend.entity.LoanSchedule;
import com.kredio.backend.entity.Payment;
import com.kredio.backend.repository.ClientRepository;
import com.kredio.backend.repository.LoanRepository;
import com.kredio.backend.repository.LoanScheduleRepository;
import com.kredio.backend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanService {

    private final LoanRepository loanRepository;
    private final LoanScheduleRepository scheduleRepository;
    private final ClientRepository clientRepository;
    private final PaymentRepository paymentRepository; // ✅ Corregido a minúscula (convención Java)
    private final AmortizationFactory amortizationFactory;

    @Transactional
    public Loan createLoan(LoanRequest request, UUID tenantId) {
        log.info(" Creando préstamo para cliente: {}, monto: {}", request.clientId(), request.principalAmount());

        // 1. Validar que el cliente existe
        Client client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        if (!client.getTenantId().equals(tenantId)) {
            throw new RuntimeException("El cliente no pertenece a esta financiera");
        }

        // 2. Parsear enums con valores por defecto
        AmortizationMethod method = AmortizationMethod.valueOf(request.amortizationMethod().toUpperCase());
        PaymentFrequency frequency = PaymentFrequency.valueOf(request.paymentFrequency().toUpperCase());
        RateType rateType = RateType.valueOf(request.rateType().toUpperCase());
        DayCountConvention dayCount = request.dayCountConvention() != null
                ? DayCountConvention.valueOf(request.dayCountConvention().toUpperCase())
                : DayCountConvention.ACTUAL_360;
        EarlyRepaymentPolicy policy = request.earlyRepaymentPolicy() != null
                ? EarlyRepaymentPolicy.valueOf(request.earlyRepaymentPolicy().toUpperCase())
                : EarlyRepaymentPolicy.WITH_CONDONATION;

        // 3. Calcular comisión si aplica
        BigDecimal principal = request.principalAmount();
        BigDecimal commission = request.administrativeFee() != null ? request.administrativeFee() : BigDecimal.ZERO;
        BigDecimal netPrincipal = principal;

        if (commission.compareTo(BigDecimal.ZERO) > 0 && "DEDUCTED".equals(request.feeCollectionMode())) {
            netPrincipal = principal.subtract(commission);
        }

        // 4. Crear la entidad Loan
        Loan loan = Loan.builder()
                .tenantId(tenantId)
                .clientId(request.clientId())
                .portfolioId(request.portfolioId() != null ? request.portfolioId() : client.getPortfolioId())
                .principalAmount(netPrincipal)
                .interestRate(request.interestRate())
                .rateType(rateType)
                .paymentFrequency(frequency)
                .amortizationMethod(method)
                .termMonths(request.term())  // ✅ CORREGIDO: usar termMonths
                .openingDate(request.openingDate())
                // ✅ ELIMINADO: firstPaymentDate (no existe en la entidad)
                .dayCountConvention(dayCount)
                .earlyRepaymentPolicy(policy)
                .graceDays(request.graceDays() != null ? request.graceDays() : 0)
                .adjustForBusinessDays(request.adjustForBusinessDays() != null ? request.adjustForBusinessDays() : false)
                // ✅ NUEVOS CAMPOS
                .disbursementMethod(request.disbursementMethod() != null
                        ? Loan.DisbursementMethod.valueOf(request.disbursementMethod())
                        : Loan.DisbursementMethod.CASH)
                .administrativeFee(commission)
                .feeCollectionMode(request.feeCollectionMode() != null
                        ? Loan.FeeCollectionMode.valueOf(request.feeCollectionMode())
                        : Loan.FeeCollectionMode.DEDUCTED)
                .moratoryType(request.moratoryType() != null
                        ? Loan.MoratoryType.valueOf(request.moratoryType())
                        : Loan.MoratoryType.PERCENTAGE)
                .moratoryValue(request.moratoryValue() != null ? request.moratoryValue() : BigDecimal.ZERO)
                .status(Loan.LoanStatus.PENDING)
                .approvalStatus(Loan.ApprovalStatus.PENDING)
                .currentBalance(netPrincipal)
                .build();

        loan = loanRepository.save(loan);
        log.info("✅ Préstamo creado en estado PENDING con ID: {}", loan.getId());

        // 5. Generar calendario de pagos
        try {
            AmortizationRequest amortizationRequest = AmortizationRequest.builder()
                    .principalAmount(netPrincipal)
                    .annualInterestRate(request.interestRate())
                    .termMonths(request.term())
                    .amortizationMethod(method)
                    .paymentFrequency(frequency)
                    .rateType(rateType)
                    .dayCountConvention(dayCount)
                    .startDate(request.openingDate())  // ✅ Usar openingDate como startDate
                    .graceDays(request.graceDays() != null ? request.graceDays() : 0)
                    .adjustForBusinessDays(request.adjustForBusinessDays() != null ? request.adjustForBusinessDays() : false)
                    .earlyRepaymentPolicy(policy)
                    .build();

            AmortizationEngine engine = amortizationFactory.getEngine(method);
            List<AmortizationScheduleDTO> scheduleDTOs = engine.generateSchedule(amortizationRequest);

            // 6. Guardar cuotas en la base de datos
            for (AmortizationScheduleDTO dto : scheduleDTOs) {
                LoanSchedule schedule = LoanSchedule.builder()
                        .loanId(loan.getId())
                        .tenantId(tenantId)
                        .installmentNumber(dto.getInstallmentNumber())
                        .dueDate(dto.getDueDate())
                        .expectedAmount(dto.getInstallmentAmount())
                        .expectedPrincipal(dto.getPrincipalAmount())
                        .expectedInterest(dto.getInterestAmount())
                        .status(LoanSchedule.ScheduleStatus.PENDING)
                        .build();
                scheduleRepository.save(schedule);  // ✅ CORREGIDO: usar scheduleRepository
            }

            log.info("✅ Calendario generado: {} cuotas", scheduleDTOs.size());
        } catch (Exception e) {
            log.error("❌ Error al generar calendario: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar el calendario de pagos: " + e.getMessage());
        }

        return loan;
    }

    @Transactional
    public void deleteLoan(UUID loanId, UUID tenantId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));

        if (!loan.getTenantId().equals(tenantId)) {
            throw new RuntimeException("El préstamo no pertenece a esta financiera");
        }

        List<LoanSchedule> schedules = scheduleRepository.findByLoanId(loanId);
        scheduleRepository.deleteAll(schedules);

        List<Payment> payments = paymentRepository.findByLoanId(loanId);
        paymentRepository.deleteAll(payments);

        loanRepository.delete(loan);

        log.info("✅ Préstamo {} y toda su información relacionada eliminada", loanId);
    }

    public List<LoanResponse> getAllLoans(UUID tenantId, String status, String search) {
        List<Loan> loans = (status != null && !status.isEmpty())
                ? loanRepository.findByTenantIdAndStatus(tenantId, Loan.LoanStatus.valueOf(status.toUpperCase()))
                : loanRepository.findByTenantId(tenantId);

        return loans.stream().map(loan -> {
                    String clientName = clientRepository.findById(loan.getClientId())
                            .map(Client::getFullName).orElse("Cliente no encontrado");
                    return new LoanResponse(
                            loan.getId(),
                            loan.getClientId(),
                            clientName,
                            loan.getPrincipalAmount(),
                            loan.getInterestRate(),
                            loan.getTermMonths(),
                            loan.getStatus().name(),
                            loan.getCurrentBalance(),
                            loan.getOpeningDate(),
                            loan.getCreatedAt(),
                            loan.getDayCountConvention() != null ? loan.getDayCountConvention().name() : "ACTUAL_360",
                            loan.getEarlyRepaymentPolicy() != null ? loan.getEarlyRepaymentPolicy().name() : "WITH_CONDONATION",
                            loan.getGraceDays() != null ? loan.getGraceDays() : 0,
                            loan.getAdjustForBusinessDays() != null ? loan.getAdjustForBusinessDays() : false
                    );
                }).filter(loan -> search == null || search.isEmpty() || loan.clientName().toLowerCase().contains(search.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<LoanScheduleResponse> getLoanSchedule(UUID loanId, UUID tenantId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));
        if (!loan.getTenantId().equals(tenantId)) throw new RuntimeException("Acceso denegado");

        return scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId).stream()
                .map(s -> LoanScheduleResponse.builder()
                        .id(s.getId())
                        .installmentNumber(s.getInstallmentNumber())
                        .dueDate(s.getDueDate())
                        .expectedAmount(s.getExpectedAmount())
                        .expectedPrincipal(s.getExpectedPrincipal())
                        .expectedInterest(s.getExpectedInterest())
                        .status(s.getStatus().name())
                        .build())
                .collect(Collectors.toList());
    }

    public List<AmortizationScheduleDTO> simulateLoan(LoanRequest request, UUID tenantId) {
        // Validar campos requeridos
        if (request.principalAmount() == null || request.principalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El monto principal es requerido");
        }
        if (request.interestRate() == null || request.interestRate().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("La tasa de interés es requerida");
        }
        if (request.term() == null || request.term() <= 0) {
            throw new RuntimeException("El número de cuotas es requerido");
        }

        // Calcular comisión si aplica
        BigDecimal principal = request.principalAmount();
        BigDecimal commission = request.administrativeFee() != null ? request.administrativeFee() : BigDecimal.ZERO;

        BigDecimal netPrincipal = principal;
        if (commission.compareTo(BigDecimal.ZERO) > 0) {
            if ("DEDUCTED".equals(request.feeCollectionMode())) {
                netPrincipal = principal.subtract(commission);
            }
            // Si es FINANCED, se agrega a las cuotas (lógica adicional necesaria)
        }

        // Parsear enums
        AmortizationMethod method = AmortizationMethod.valueOf(request.amortizationMethod());
        PaymentFrequency frequency = PaymentFrequency.valueOf(request.paymentFrequency());
        RateType rateType = RateType.valueOf(request.rateType());
        DayCountConvention dayCount = DayCountConvention.valueOf(request.dayCountConvention());
        EarlyRepaymentPolicy policy = request.earlyRepaymentPolicy() != null
                ? EarlyRepaymentPolicy.valueOf(request.earlyRepaymentPolicy())
                : EarlyRepaymentPolicy.WITH_CONDONATION;

        // ✅ CORRECCIÓN 1: Usar Builder en lugar de constructor
        AmortizationRequest amortizationRequest = AmortizationRequest.builder()
                .principalAmount(netPrincipal)
                .annualInterestRate(request.interestRate())
                .termMonths(request.term())
                .amortizationMethod(method)
                .paymentFrequency(frequency)
                .rateType(rateType)
                .dayCountConvention(dayCount)
                .startDate(request.openingDate())
                .graceDays(request.graceDays() != null ? request.graceDays() : 0)
                .adjustForBusinessDays(request.adjustForBusinessDays() != null ? request.adjustForBusinessDays() : false)
                .earlyRepaymentPolicy(policy)
                .build();

        // ✅ CORRECCIÓN 2: Usar getEngine() en lugar de create()
        AmortizationEngine engine = amortizationFactory.getEngine(method);

        return engine.generateSchedule(amortizationRequest);
    }
}