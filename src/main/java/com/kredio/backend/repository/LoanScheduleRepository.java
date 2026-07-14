package com.kredio.backend.repository;

import com.kredio.backend.entity.LoanSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface LoanScheduleRepository extends JpaRepository<LoanSchedule, UUID> {

    List<LoanSchedule> findByLoanIdAndStatusOrderByDueDateAsc(UUID loanId, LoanSchedule.ScheduleStatus status);

    List<LoanSchedule> findByLoanIdOrderByInstallmentNumberAsc(UUID loanId);

    @Query("""
    SELECT COALESCE(SUM(ls.expectedAmount), 0) 
    FROM LoanSchedule ls 
    WHERE ls.tenantId = :tenantId 
    AND ls.status = 'PENDING'
    AND ls.dueDate BETWEEN :startDate AND :endDate
""")
    BigDecimal sumExpectedPaymentsBetween(
            @Param("tenantId") UUID tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
    SELECT COUNT(ls) 
    FROM LoanSchedule ls 
    WHERE ls.tenantId = :tenantId 
    AND ls.status = 'PENDING'
    AND ls.dueDate BETWEEN :startDate AND :endDate
""")
    int countPendingSchedulesBetween(
            @Param("tenantId") UUID tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
    SELECT COALESCE(SUM(ls.expectedAmount), 0) 
    FROM LoanSchedule ls 
    WHERE ls.tenantId = :tenantId 
    AND ls.status IN ('PENDING', 'OVERDUE')
    AND ls.dueDate < :today
""")
    BigDecimal sumOverdueAmount(@Param("tenantId") UUID tenantId, @Param("today") LocalDate today);

    @Query("""
    SELECT COUNT(ls) 
    FROM LoanSchedule ls 
    WHERE ls.tenantId = :tenantId 
    AND ls.status IN ('PENDING', 'OVERDUE')
    AND ls.dueDate < :today
""")
    int countOverdueSchedules(@Param("tenantId") UUID tenantId, @Param("today") LocalDate today);
}
