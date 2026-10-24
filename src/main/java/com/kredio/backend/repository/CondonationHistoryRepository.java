package com.kredio.backend.repository;

import com.kredio.backend.entity.CondonationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface CondonationHistoryRepository extends JpaRepository<CondonationHistory, UUID> {

    List<CondonationHistory> findByLoanIdOrderByCreatedAtDesc(UUID loanId);

    List<CondonationHistory> findByPaymentId(UUID paymentId);

    @Query("SELECT COALESCE(SUM(c.condonedAmount), 0) FROM CondonationHistory c WHERE c.loanId = :loanId")
    BigDecimal sumCondonedAmountByLoanId(@Param("loanId") UUID loanId);

    @Query("SELECT COALESCE(SUM(c.condonedInterest), 0) FROM CondonationHistory c WHERE c.loanId = :loanId")
    BigDecimal sumCondonedInterestByLoanId(@Param("loanId") UUID loanId);
}