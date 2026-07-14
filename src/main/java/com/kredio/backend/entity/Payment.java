package com.kredio.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "loan_id", nullable = false)
    private UUID loanId;

    @Column(name = "collector_id", nullable = false)
    private UUID collectorId;

    @Column(name = "amount_paid", nullable = false, precision = 19, scale = 4)
    private BigDecimal amountPaid;

    @CreationTimestamp
    @Column(name = "payment_date", updatable = false)
    private Instant paymentDate;

    @Column(name = "method", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    @Column(name = "payment_method_id")
    private UUID paymentMethodId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "applied_to", columnDefinition = "jsonb")
    private String appliedTo;

    @Column(name = "excess_amount", precision = 19, scale = 4)
    private BigDecimal excessAmount;

    @Column(name = "is_printed")
    private Boolean isPrinted;

    @Column(name = "synced_from_offline")
    private Boolean syncedFromOffline;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public enum PaymentMethod {
        CASH, TRANSFER, CARD, OTHER
    }
}