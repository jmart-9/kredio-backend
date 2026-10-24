package com.kredio.backend.service;

import com.kredio.backend.entity.PaymentLedger;
import com.kredio.backend.repository.PaymentLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentLedgerService {

    private final PaymentLedgerRepository ledgerRepository;

    @Transactional
    public void recordEntry(
            UUID tenantId,
            UUID paymentId,
            UUID scheduleId,
            String entryType,
            BigDecimal allocatedToMoratory,
            BigDecimal allocatedToInterest,
            BigDecimal allocatedToPrincipal,
            BigDecimal totalAllocated,
            BigDecimal previousBalance,
            BigDecimal currentBalance,
            UUID createdBy,
            String reason) {

        PaymentLedger entry = PaymentLedger.builder()
                .tenantId(tenantId)
                .paymentId(paymentId)
                .scheduleId(scheduleId)
                .entryType(PaymentLedger.EntryType.valueOf(entryType))
                .allocatedToMoratory(allocatedToMoratory != null ? allocatedToMoratory : BigDecimal.ZERO)
                .allocatedToInterest(allocatedToInterest != null ? allocatedToInterest : BigDecimal.ZERO)
                .allocatedToPrincipal(allocatedToPrincipal != null ? allocatedToPrincipal : BigDecimal.ZERO)
                .totalAllocated(totalAllocated)
                .previousBalance(previousBalance)
                .currentBalance(currentBalance)
                .createdBy(createdBy)
                .reason(reason)
                .createdAt(Instant.now())
                .build();

        ledgerRepository.save(entry);
        log.info(" Ledger entry {}: payment={}, type={}, total={}",
                entry.getLedgerId(), paymentId, entryType, totalAllocated);
    }

    @Transactional
    public void recordReversal(
            UUID tenantId,
            UUID originalPaymentId,
            UUID scheduleId,
            BigDecimal allocatedToMoratory,
            BigDecimal allocatedToInterest,
            BigDecimal allocatedToPrincipal,
            BigDecimal totalAllocated,
            BigDecimal previousBalance,
            BigDecimal currentBalance,
            UUID annulledBy,
            String annulmentReason) {

        recordEntry(
                tenantId,
                originalPaymentId,
                scheduleId,
                "REVERSAL",
                allocatedToMoratory.negate(),
                allocatedToInterest.negate(),
                allocatedToPrincipal.negate(),
                totalAllocated.negate(),
                previousBalance,
                currentBalance,
                annulledBy,
                "ANULACIÓN: " + annulmentReason
        );
    }
}