package com.kredio.backend.repository;

import com.kredio.backend.entity.PaymentLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentLedgerRepository extends JpaRepository<PaymentLedger, UUID> {
    List<PaymentLedger> findByPaymentIdOrderByCreatedAtDesc(UUID paymentId);
    List<PaymentLedger> findByScheduleIdOrderByCreatedAtDesc(UUID scheduleId);
    List<PaymentLedger> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}