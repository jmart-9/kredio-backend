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

    // ✅ Buscar por loanId ordenado por número de cuota
    List<LoanSchedule> findByLoanIdOrderByInstallmentNumberAsc(UUID loanId);

    // ✅ Buscar por loanId y status ordenado por fecha de vencimiento
    List<LoanSchedule> findByLoanIdAndStatusOrderByDueDateAsc(
            UUID loanId, LoanSchedule.ScheduleStatus status);

    // ✅ Buscar por tenantId y status
    List<LoanSchedule> findByTenantIdAndStatus(
            UUID tenantId, LoanSchedule.ScheduleStatus status);

    // ✅ Buscar por tenantId con fecha de vencimiento anterior a una fecha
    List<LoanSchedule> findByTenantIdAndDueDateBeforeAndStatus(
            UUID tenantId, LocalDate dueDate, LoanSchedule.ScheduleStatus status);

    // ✅ Sumar montos de cuotas vencidas (PENDING con dueDate < hoy)
    @Query("""
        SELECT COALESCE(SUM(ls.expectedAmount), 0)
        FROM LoanSchedule ls
        WHERE ls.tenantId = :tenantId
        AND ls.status = 'PENDING'
        AND ls.dueDate < :today
    """)
    BigDecimal sumOverdueAmount(
            @Param("tenantId") UUID tenantId,
            @Param("today") LocalDate today);

    // ✅ Sumar pagos esperados entre dos fechas
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
            @Param("endDate") LocalDate endDate);

    // ✅ Contar cuotas pendientes entre dos fechas
    @Query("""
        SELECT COUNT(ls.id)
        FROM LoanSchedule ls
        WHERE ls.tenantId = :tenantId
        AND ls.status = 'PENDING'
        AND ls.dueDate BETWEEN :startDate AND :endDate
    """)
    long countPendingSchedulesBetween(
            @Param("tenantId") UUID tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // ✅ Buscar por loanId (sin orden específico)
    List<LoanSchedule> findByLoanId(UUID loanId);
}