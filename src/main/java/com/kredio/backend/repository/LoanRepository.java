package com.kredio.backend.repository;

import com.kredio.backend.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface LoanRepository extends JpaRepository<Loan, UUID> {

    @Query("SELECT l FROM Loan l WHERE l.tenantId = :tenantId ORDER BY l.createdAt DESC")
    List<Loan> findByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT l FROM Loan l WHERE l.tenantId = :tenantId AND l.status = :status ORDER BY l.createdAt DESC")
    List<Loan> findByTenantIdAndStatus(@Param("tenantId") UUID tenantId, @Param("status") Loan.LoanStatus status);

    @Query("SELECT l FROM Loan l WHERE l.tenantId = :tenantId AND l.status = :status ORDER BY l.createdAt DESC")
    List<Loan> findByTenantIdAndStatusString(@Param("tenantId") UUID tenantId, @Param("status") String status);

    @Query("SELECT COALESCE(SUM(l.currentBalance), 0) FROM Loan l WHERE l.tenantId = :tenantId AND l.status = 'ACTIVE'")
    BigDecimal sumActivePortfolio(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.tenantId = :tenantId AND l.status = 'ACTIVE'")
    int countActiveLoans(@Param("tenantId") UUID tenantId);

    @Query("SELECT l FROM Loan l WHERE l.tenantId = :tenantId AND l.clientId = :clientId ORDER BY l.createdAt DESC")
    List<Loan> findByTenantIdAndClientId(@Param("tenantId") UUID tenantId, @Param("clientId") UUID clientId);

    @Query("SELECT l FROM Loan l WHERE l.tenantId = :tenantId AND l.approvalStatus = :approvalStatus ORDER BY l.createdAt DESC")
    List<Loan> findByTenantIdAndApprovalStatus(@Param("tenantId") UUID tenantId, @Param("approvalStatus") Loan.ApprovalStatus approvalStatus);


    @Query("""
        SELECT DISTINCT l FROM Loan l 
        JOIN LoanSchedule ls ON ls.loanId = l.id
        WHERE l.tenantId = :tenantId 
        AND l.status = 'ACTIVE'
        AND ls.status = 'OVERDUE'
        AND ls.dueDate <= :cutoffDate
    """)
    List<Loan> findLoansWithOverdueBefore(@Param("tenantId") UUID tenantId, @Param("cutoffDate") LocalDate cutoffDate);
}