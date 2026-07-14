package com.kredio.backend.service;

import com.kredio.backend.dto.reports.CashflowProjectionResponse;
import com.kredio.backend.dto.reports.CollectionPerformanceResponse;
import com.kredio.backend.dto.reports.PortfolioAtRiskResponse;
import com.kredio.backend.entity.Loan;
import com.kredio.backend.entity.User;
import com.kredio.backend.repository.LoanRepository;
import com.kredio.backend.repository.LoanScheduleRepository;
import com.kredio.backend.repository.PaymentRepository;
import com.kredio.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final LoanRepository loanRepository;
    private final LoanScheduleRepository scheduleRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    /**
     * Calcula PAR 30, 60 y 90
     * PAR = Portfolio at Risk (Cartera en Riesgo)
     */
    public PortfolioAtRiskResponse getPortfolioAtRisk(UUID tenantId) {
        LocalDate today = LocalDate.now();
        LocalDate cutoff30 = today.minusDays(30);
        LocalDate cutoff60 = today.minusDays(60);
        LocalDate cutoff90 = today.minusDays(90);

        // 1. Obtener cartera total activa
        BigDecimal totalPortfolio = loanRepository.sumActivePortfolio(tenantId);
        int totalActiveLoans = loanRepository.countActiveLoans(tenantId);

        // 2. Obtener préstamos con cuotas vencidas
        List<Loan> allActiveLoans = loanRepository.findByTenantId(tenantId)
                .stream()
                .filter(l -> l.getStatus() == Loan.LoanStatus.ACTIVE)
                .toList();

        BigDecimal par30Amount = BigDecimal.ZERO;
        BigDecimal par60Amount = BigDecimal.ZERO;
        BigDecimal par90Amount = BigDecimal.ZERO;
        int loansWithOverdue30 = 0;
        int loansWithOverdue60 = 0;
        int loansWithOverdue90 = 0;

        for (Loan loan : allActiveLoans) {
            // Buscar la cuota vencida más antigua del préstamo
            LocalDate oldestOverdueDate = findOldestOverdueDate(loan.getId());

            if (oldestOverdueDate != null) {
                long daysOverdue = ChronoUnit.DAYS.between(oldestOverdueDate, today);

                if (daysOverdue > 90) {
                    par90Amount = par90Amount.add(loan.getCurrentBalance());
                    par60Amount = par60Amount.add(loan.getCurrentBalance());
                    par30Amount = par30Amount.add(loan.getCurrentBalance());
                    loansWithOverdue90++;
                    loansWithOverdue60++;
                    loansWithOverdue30++;
                } else if (daysOverdue > 60) {
                    par60Amount = par60Amount.add(loan.getCurrentBalance());
                    par30Amount = par30Amount.add(loan.getCurrentBalance());
                    loansWithOverdue60++;
                    loansWithOverdue30++;
                } else if (daysOverdue > 30) {
                    par30Amount = par30Amount.add(loan.getCurrentBalance());
                    loansWithOverdue30++;
                }
            }
        }

        // 3. Calcular porcentajes
        BigDecimal par30Pct = calculatePercentage(par30Amount, totalPortfolio);
        BigDecimal par60Pct = calculatePercentage(par60Amount, totalPortfolio);
        BigDecimal par90Pct = calculatePercentage(par90Amount, totalPortfolio);

        return new PortfolioAtRiskResponse(
                today,
                totalPortfolio,
                par30Amount,
                par60Amount,
                par90Amount,
                par30Pct,
                par60Pct,
                par90Pct,
                totalActiveLoans,
                loansWithOverdue30,
                loansWithOverdue60,
                loansWithOverdue90
        );
    }

    private LocalDate findOldestOverdueDate(UUID loanId) {
        // Busca todas las cuotas no pagadas y ordenadas por fecha
        var schedules = scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId);

        LocalDate oldest = null;
        LocalDate today = LocalDate.now();

        for (var schedule : schedules) {
            String status = schedule.getStatus().name();
            if ("PENDING".equals(status) || "OVERDUE".equals(status)) {
                if (schedule.getDueDate().isBefore(today)) {
                    if (oldest == null || schedule.getDueDate().isBefore(oldest)) {
                        oldest = schedule.getDueDate();
                    }
                }
            }
        }

        return oldest;
    }

    private BigDecimal calculatePercentage(BigDecimal amount, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(BigDecimal.valueOf(100))
                .divide(total, 2, RoundingMode.HALF_UP);
    }

    /**
     * Resumen de cobranza por cobrador
     */
    public List<CollectionPerformanceResponse> getCollectionPerformance(UUID tenantId, int days) {
        Instant endDate = Instant.now();
        Instant startDate = endDate.minus(days, ChronoUnit.DAYS);

        List<Object[]> stats = paymentRepository.getCollectionStatsByCollector(tenantId, startDate, endDate);
        List<CollectionPerformanceResponse> response = new ArrayList<>();

        for (Object[] row : stats) {
            UUID collectorId = (UUID) row[0];
            int totalVisits = ((Number) row[1]).intValue();
            BigDecimal totalCollected = (BigDecimal) row[2];

            User collector = userRepository.findById(collectorId).orElse(null);
            String collectorName = collector != null ? collector.getFullName() : "Desconocido";

            BigDecimal averageTicket = totalVisits > 0
                    ? totalCollected.divide(BigDecimal.valueOf(totalVisits), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // Por ahora simplificamos la eficiencia (requeriría préstamos asignados)
            BigDecimal efficiency = BigDecimal.ZERO;

            response.add(new CollectionPerformanceResponse(
                    collectorId,
                    collectorName,
                    totalVisits,
                    totalVisits, // Simplificado: asumimos todos exitosos
                    totalCollected,
                    averageTicket,
                    0, // Asignados: requiere refactor futuro
                    efficiency
            ));
        }

        return response;
    }

    /**
     * Proyección de flujo de caja
     */
    public CashflowProjectionResponse getCashflowProjection(UUID tenantId) {
        LocalDate today = LocalDate.now();
        LocalDate in30 = today.plusDays(30);
        LocalDate in60 = today.plusDays(60);
        LocalDate in90 = today.plusDays(90);

        // Montos esperados (cuotas pendientes por vencer)
        BigDecimal expected30 = scheduleRepository.sumExpectedPaymentsBetween(tenantId, today, in30);
        BigDecimal expected60 = scheduleRepository.sumExpectedPaymentsBetween(tenantId, today, in60);
        BigDecimal expected90 = scheduleRepository.sumExpectedPaymentsBetween(tenantId, today, in90);

        int count30 = scheduleRepository.countPendingSchedulesBetween(tenantId, today, in30);
        int count60 = scheduleRepository.countPendingSchedulesBetween(tenantId, today, in60);
        int count90 = scheduleRepository.countPendingSchedulesBetween(tenantId, today, in90);

        // Montos vencidos (cuotas que ya debieron pagarse)
        BigDecimal overdueAmount = scheduleRepository.sumOverdueAmount(tenantId, today);
        int overdueCount = scheduleRepository.countOverdueSchedules(tenantId, today);

        return new CashflowProjectionResponse(
                expected30,
                expected60,
                expected90,
                count30,
                count60,
                count90,
                overdueAmount,
                overdueCount
        );
    }
}
