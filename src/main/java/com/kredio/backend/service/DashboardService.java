package com.kredio.backend.service;

import com.kredio.backend.dto.DashboardStatsDTO;
import com.kredio.backend.entity.Client;
import com.kredio.backend.entity.Loan;
import com.kredio.backend.entity.LoanSchedule;
import com.kredio.backend.repository.ClientRepository;
import com.kredio.backend.repository.LoanRepository;
import com.kredio.backend.repository.LoanScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ClientRepository clientRepository;
    private final LoanRepository loanRepository;
    private final LoanScheduleRepository scheduleRepository;

    public DashboardStatsDTO getDashboardStats(UUID tenantId) {
        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusDays(7);
        LocalDate startOfMonth = today.withDayOfMonth(1);

        // 1. Métricas básicas
        List<Client> clients = clientRepository.findByTenantId(tenantId);
        List<Loan> allLoans = loanRepository.findByTenantId(tenantId);

        long totalLoans = allLoans.size();
        long activeLoans = allLoans.stream().filter(l -> l.getStatus() == Loan.LoanStatus.ACTIVE).count();
        long paidLoans = allLoans.stream().filter(l -> l.getStatus() == Loan.LoanStatus.PAID).count();
        long overdueLoans = allLoans.stream().filter(l -> l.getStatus() == Loan.LoanStatus.DEFAULTED).count();

        // 2. Montos de cartera
        BigDecimal totalPortfolio = allLoans.stream()
                .map(Loan::getPrincipalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal activePortfolio = loanRepository.sumActivePortfolio(tenantId);
        BigDecimal overdueAmount = scheduleRepository.sumOverdueAmount(tenantId, today);

        // 3. Próximos vencimientos (7 días)
        BigDecimal upcomingAmount = scheduleRepository.sumExpectedPaymentsBetween(tenantId, today, nextWeek);
        int upcomingCount = (int) scheduleRepository.countPendingSchedulesBetween(tenantId, today, nextWeek);

        // 4. Lista detallada de próximos vencimientos
        List<DashboardStatsDTO.UpcomingPaymentDTO> upcomingPayments = buildUpcomingPayments(tenantId, today, nextWeek);

        // 5. Datos para gráfico mensual (últimos 6 meses)
        List<DashboardStatsDTO.MonthlyDataDTO> monthlyData = buildMonthlyData(tenantId);

        // 6. Distribución por estado
        Map<String, Long> loansByStatus = allLoans.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getStatus().name(),
                        Collectors.counting()
                ));

        // 7. Monto recaudado (estimado: total prestado - saldo actual de activos)
        BigDecimal collectedAmount = totalPortfolio.subtract(activePortfolio).max(BigDecimal.ZERO);

        return new DashboardStatsDTO(
                clients.size(),
                totalLoans,
                activeLoans,
                paidLoans,
                overdueLoans,
                totalPortfolio,
                activePortfolio,
                overdueAmount,
                collectedAmount,
                upcomingAmount,
                upcomingCount,
                upcomingPayments,
                monthlyData,
                loansByStatus
        );
    }

    private List<DashboardStatsDTO.UpcomingPaymentDTO> buildUpcomingPayments(UUID tenantId, LocalDate from, LocalDate to) {
        List<Loan> activeLoans = loanRepository.findByTenantIdAndStatus(tenantId, Loan.LoanStatus.ACTIVE);
        List<DashboardStatsDTO.UpcomingPaymentDTO> result = new ArrayList<>();

        for (Loan loan : activeLoans) {
            List<LoanSchedule> schedules = scheduleRepository.findByLoanIdAndStatusOrderByDueDateAsc(
                    loan.getId(), LoanSchedule.ScheduleStatus.PENDING
            );

            Client client = clientRepository.findById(loan.getClientId()).orElse(null);
            String clientName = client != null ? client.getFullName() : "Cliente desconocido";

            for (LoanSchedule schedule : schedules) {
                if (!schedule.getDueDate().isBefore(from) && !schedule.getDueDate().isAfter(to)) {
                    result.add(new DashboardStatsDTO.UpcomingPaymentDTO(
                            loan.getId().toString(),
                            clientName,
                            schedule.getDueDate().toString(),
                            schedule.getExpectedAmount(),
                            schedule.getInstallmentNumber()
                    ));
                }
            }
        }

        return result.stream()
                .sorted(Comparator.comparing(DashboardStatsDTO.UpcomingPaymentDTO::dueDate))
                .limit(5) // Top 5 próximos
                .collect(Collectors.toList());
    }

    private List<DashboardStatsDTO.MonthlyDataDTO> buildMonthlyData(UUID tenantId) {
        List<Loan> allLoans = loanRepository.findByTenantId(tenantId);
        List<DashboardStatsDTO.MonthlyDataDTO> result = new ArrayList<>();

        YearMonth currentMonth = YearMonth.now();

        // Últimos 6 meses
        for (int i = 5; i >= 0; i--) {
            YearMonth month = currentMonth.minusMonths(i);
            LocalDate monthStart = month.atDay(1);
            LocalDate monthEnd = month.atEndOfMonth();

            BigDecimal disbursed = allLoans.stream()
                    .filter(l -> l.getOpeningDate() != null
                            && !l.getOpeningDate().isBefore(monthStart)
                            && !l.getOpeningDate().isAfter(monthEnd))
                    .map(Loan::getPrincipalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            result.add(new DashboardStatsDTO.MonthlyDataDTO(
                    month.format(DateTimeFormatter.ofPattern("MMM")),
                    disbursed,
                    BigDecimal.ZERO // TODO: Implementar cuando haya tabla de pagos
            ));
        }

        return result;
    }
}