package com.kredio.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "loan_id", nullable = false)
    private UUID loanId;

    @Column(name = "schedule_id")
    private UUID scheduleId;

    @Column(name = "collector_id", nullable = false)
    private UUID collectorId;

    @Column(name = "amount_paid", nullable = false, precision = 19, scale = 4)
    private BigDecimal amountPaid;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private PaymentMethod method;

    @Column(name = "payment_date", nullable = false)
    private Instant paymentDate;

    @Column(name = "is_printed")
    private Boolean isPrinted;

    @Column(name = "synced_from_offline")
    private Boolean syncedFromOffline;

    @Column(name = "is_voided")
    private Boolean isVoided;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Column(name = "voided_by")
    private UUID voidedBy;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "applied_to_principal", precision = 19, scale = 4)
    private BigDecimal appliedToPrincipal;

    @Column(name = "applied_to_moratory", precision = 19, scale = 4)
    private BigDecimal appliedToMoratory;

    @Column(name = "applied_to_interest", precision = 19, scale = 4)
    private BigDecimal appliedToInterest;

    @Column(name = "excess_amount", precision = 19, scale = 4)
    private BigDecimal excessAmount;

    // ✅ NUEVOS CAMPOS para anulación (Fase 1)
    @Column(name = "annulment_reason", length = 500)
    private String annulmentReason;

    @Column(name = "annulled_by_user_id")
    private UUID annulledByUserId;

    @Column(name = "annulled_at")
    private Instant annulledAt;

    @Column(name = "client_transaction_uuid", unique = true)
    private UUID clientTransactionUuid;

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    public enum PaymentMethod {
        CASH, TRANSFER, CHECK
    }
}