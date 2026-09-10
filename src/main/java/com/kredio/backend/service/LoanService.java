package com.kredio.backend.service;

import com.kredio.backend.dto.LoanRequest;
import com.kredio.backend.dto.LoanResponse;
import com.kredio.backend.dto.LoanScheduleResponse;
import com.kredio.backend.entity.Client;
import com.kredio.backend.entity.Loan;
import com.kredio.backend.entity.LoanSchedule;
import com.kredio.backend.repository.ClientRepository;
import com.kredio.backend.repository.LoanRepository;
import com.kredio.backend.repository.LoanScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
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

    @Transactional
    public Loan createLoan(LoanRequest request, UUID tenantId) {
        Client client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        if (!client.getTenantId().equals(tenantId)) {
            throw new RuntimeException("El cliente no pertenece a esta financiera");
        }

        // ✅ CAMBIO: Crear préstamo en estado PENDING con approvalStatus PENDING
        Loan loan = Loan.builder()
                .tenantId(tenantId)
                .clientId(request.clientId())
                .portfolioId(client.getPortfolioId())
                .principalAmount(request.principalAmount())
                .interestRate(request.interestRate())
                .rateType(request.rateType())
                .paymentFrequency(request.paymentFrequency())
                .amortizationMethod(request.amortizationMethod())
                .openingDate(request.openingDate())
                .termMonths(request.term())
                .currentBalance(request.principalAmount())
                .status(Loan.LoanStatus.PENDING)  // ✅ CAMBIADO de ACTIVE a PENDING
                .approvalStatus(Loan.ApprovalStatus.PENDING)  // ✅ AGREGADO
                .build();

        loan = loanRepository.save(loan);

        // Generar calendario de pagos (igual que antes)
        List<LoanSchedule> schedules;
        if ("SIMPLE".equals(request.amortizationMethod())) {
            schedules = calculateSimpleAmortization(
                    request.principalAmount(), request.interestRate(), request.rateType(),
                    request.paymentFrequency(), request.term(), request.firstPaymentDate(),
                    loan.getId(), tenantId
            );
        } else {
            schedules = calculateFrenchAmortization(
                    request.principalAmount(), request.interestRate(), request.rateType(),
                    request.paymentFrequency(), request.term(), request.firstPaymentDate(),
                    loan.getId(), tenantId
            );
        }

        scheduleRepository.saveAll(schedules);

        log.info("✅ Préstamo creado en estado PENDING: {}", loan.getId());
        return loan;
    }

    private List<LoanSchedule> calculateFrenchAmortization(BigDecimal principal, BigDecimal interestRate, String rateType, String paymentFrequency, int term, LocalDate firstPaymentDate, UUID loanId, UUID tenantId) {
        List<LoanSchedule> schedules = new ArrayList<>();
        BigDecimal remainingBalance = principal;
        BigDecimal periodRate = calculatePeriodRate(interestRate, rateType, paymentFrequency);
        int totalPayments = term;

        BigDecimal payment;
        if (periodRate.compareTo(BigDecimal.ZERO) == 0) {
            payment = principal.divide(BigDecimal.valueOf(totalPayments), 4, RoundingMode.HALF_UP);
        } else {
            BigDecimal onePlusRate = BigDecimal.ONE.add(periodRate);
            BigDecimal numerator = periodRate.multiply(onePlusRate.pow(totalPayments));
            BigDecimal denominator = onePlusRate.pow(totalPayments).subtract(BigDecimal.ONE);
            payment = principal.multiply(numerator).divide(denominator, 4, RoundingMode.HALF_UP);
        }

        LocalDate currentDate = firstPaymentDate;
        for (int i = 1; i <= totalPayments; i++) {
            BigDecimal interest = remainingBalance.multiply(periodRate).setScale(4, RoundingMode.HALF_UP);
            BigDecimal principalPayment = payment.subtract(interest).setScale(4, RoundingMode.HALF_UP);

            if (i == totalPayments) {
                principalPayment = remainingBalance;
                payment = principalPayment.add(interest);
            }

            remainingBalance = remainingBalance.subtract(principalPayment).setScale(4, RoundingMode.HALF_UP);

            schedules.add(LoanSchedule.builder()
                    .tenantId(tenantId)
                    .loanId(loanId)
                    .installmentNumber(i)
                    .dueDate(currentDate)
                    .expectedAmount(payment)
                    .expectedPrincipal(principalPayment)
                    .expectedInterest(interest)
                    .status(LoanSchedule.ScheduleStatus.PENDING)
                    .build());

            currentDate = calculateNextDueDate(currentDate, paymentFrequency);
        }
        return schedules;
    }

    private List<LoanSchedule> calculateSimpleAmortization(BigDecimal principal, BigDecimal interestRate, String rateType, String paymentFrequency, int term, LocalDate firstPaymentDate, UUID loanId, UUID tenantId) {
        List<LoanSchedule> schedules = new ArrayList<>();
        BigDecimal periodRate = calculatePeriodRate(interestRate, rateType, paymentFrequency);
        int totalPayments = term;

        // Interés fijo sobre el capital original en cada período
        BigDecimal fixedInterest = principal.multiply(periodRate).setScale(4, RoundingMode.HALF_UP);
        BigDecimal fixedPrincipal = principal.divide(BigDecimal.valueOf(totalPayments), 4, RoundingMode.HALF_UP);
        BigDecimal payment = fixedPrincipal.add(fixedInterest).setScale(4, RoundingMode.HALF_UP);

        LocalDate currentDate = firstPaymentDate;
        BigDecimal remainingBalance = principal;

        for (int i = 1; i <= totalPayments; i++) {
            BigDecimal interest = fixedInterest;
            BigDecimal principalPayment = fixedPrincipal;

            if (i == totalPayments) {
                principalPayment = remainingBalance;
                payment = principalPayment.add(interest);
            }

            remainingBalance = remainingBalance.subtract(principalPayment).setScale(4, RoundingMode.HALF_UP);

            schedules.add(LoanSchedule.builder()
                    .tenantId(tenantId)
                    .loanId(loanId)
                    .installmentNumber(i)
                    .dueDate(currentDate)
                    .expectedAmount(payment)
                    .expectedPrincipal(principalPayment)
                    .expectedInterest(interest)
                    .status(LoanSchedule.ScheduleStatus.PENDING)
                    .build());

            currentDate = calculateNextDueDate(currentDate, paymentFrequency);
        }
        return schedules;
    }

    private BigDecimal calculatePeriodRate(BigDecimal interestRate, String rateType, String paymentFrequency) {
        BigDecimal rateDecimal = interestRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        BigDecimal monthlyRate = "MONTHLY".equals(rateType)
                ? rateDecimal
                : rateDecimal.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);

        return switch (paymentFrequency) {
            case "DAILY" -> monthlyRate.divide(BigDecimal.valueOf(30), 10, RoundingMode.HALF_UP);
            case "WEEKLY" -> monthlyRate.divide(BigDecimal.valueOf(4.33), 10, RoundingMode.HALF_UP);
            case "BIWEEKLY" -> monthlyRate.divide(BigDecimal.valueOf(2), 10, RoundingMode.HALF_UP);
            case "MONTHLY" -> monthlyRate;
            case "QUARTERLY" -> monthlyRate.multiply(BigDecimal.valueOf(3));
            case "SEMIANNUAL" -> monthlyRate.multiply(BigDecimal.valueOf(6));
            case "ANNUAL" -> monthlyRate.multiply(BigDecimal.valueOf(12));
            default -> monthlyRate;
        };
    }

    private LocalDate calculateNextDueDate(LocalDate currentDate, String paymentFrequency) {
        return switch (paymentFrequency) {
            case "DAILY" -> currentDate.plusDays(1);
            case "WEEKLY" -> currentDate.plusWeeks(1);
            case "BIWEEKLY" -> currentDate.plusWeeks(2);
            case "MONTHLY" -> currentDate.plusMonths(1);
            case "QUARTERLY" -> currentDate.plusMonths(3);
            case "SEMIANNUAL" -> currentDate.plusMonths(6);
            case "ANNUAL" -> currentDate.plusYears(1);
            default -> currentDate.plusMonths(1);
        };
    }

    public List<LoanResponse> getAllLoans(UUID tenantId, String status, String search) {
        List<Loan> loans = (status != null && !status.isEmpty())
                ? loanRepository.findByTenantIdAndStatus(tenantId, Loan.LoanStatus.valueOf(status.toUpperCase()))
                : loanRepository.findByTenantId(tenantId);

        return loans.stream().map(loan -> {
                    String clientName = clientRepository.findById(loan.getClientId())
                            .map(Client::getFullName).orElse("Cliente no encontrado");
                    return new LoanResponse(loan.getId(), loan.getClientId(), clientName, loan.getPrincipalAmount(),
                            loan.getInterestRate(), loan.getTermMonths(), loan.getStatus().name(),
                            loan.getCurrentBalance(), loan.getOpeningDate(), loan.getCreatedAt());
                }).filter(loan -> search == null || search.isEmpty() || loan.clientName().toLowerCase().contains(search.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<LoanScheduleResponse> getLoanSchedule(UUID loanId, UUID tenantId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));
        if (!loan.getTenantId().equals(tenantId)) throw new RuntimeException("Acceso denegado");

        return scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId).stream()
                .map(s -> LoanScheduleResponse.builder()
                        .id(s.getId()).installmentNumber(s.getInstallmentNumber()).dueDate(s.getDueDate())
                        .expectedAmount(s.getExpectedAmount()).expectedPrincipal(s.getExpectedPrincipal())
                        .expectedInterest(s.getExpectedInterest()).status(s.getStatus().name()).build())
                .collect(Collectors.toList());
    }
}