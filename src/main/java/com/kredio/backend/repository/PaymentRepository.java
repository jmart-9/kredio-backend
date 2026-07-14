package com.kredio.backend.repository;

import com.kredio.backend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    @Query("SELECT p FROM Payment p WHERE p.tenantId = :tenantId ORDER BY p.paymentDate DESC")
    List<Payment> findByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT p FROM Payment p WHERE p.tenantId = :tenantId AND p.loanId = :loanId ORDER BY p.paymentDate DESC")
    List<Payment> findByTenantIdAndLoanId(@Param("tenantId") UUID tenantId, @Param("loanId") UUID loanId);

    @Query("SELECT p FROM Payment p WHERE p.tenantId = :tenantId AND p.paymentDate BETWEEN :startDate AND :endDate ORDER BY p.paymentDate DESC")
    List<Payment> findByTenantIdAndDateRange(
            @Param("tenantId") UUID tenantId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    @Query("SELECT p FROM Payment p WHERE p.loanId = :loanId")
    List<Payment> findByLoanId(@Param("loanId") UUID loanId);

    @Query("""
        SELECT p.collectorId, 
                COUNT(p), 
                SUM(p.amountPaid)
        FROM Payment p 
        WHERE p.tenantId = :tenantId 
        AND p.paymentDate BETWEEN :startDate AND :endDate
        GROUP BY p.collectorId
    """)
    List<Object[]> getCollectionStatsByCollector(
            @Param("tenantId") UUID tenantId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );
}