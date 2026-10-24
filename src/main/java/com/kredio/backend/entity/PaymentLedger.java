package com.kredio.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_ledger")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PaymentLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ledger_id")
    private UUID ledgerId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "schedule_id", nullable = false)
    private UUID scheduleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 20)
    private EntryType entryType;

    @Column(name = "allocated_to_moratory", precision = 19, scale = 4)
    private BigDecimal allocatedToMoratory;

    @Column(name = "allocated_to_interest", precision = 19, scale = 4)
    private BigDecimal allocatedToInterest;

    @Column(name = "allocated_to_principal", precision = 19, scale = 4)
    private BigDecimal allocatedToPrincipal;

    @Column(name = "total_allocated", precision = 19, scale = 4, nullable = false)
    private BigDecimal totalAllocated;

    @Column(name = "previous_balance", precision = 19, scale = 4)
    private BigDecimal previousBalance;

    @Column(name = "current_balance", precision = 19, scale = 4)
    private BigDecimal currentBalance;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "created_at")
    private Instant createdAt;

    public enum EntryType {
        PAYMENT, REVERSAL, ADJUSTMENT
    }
}