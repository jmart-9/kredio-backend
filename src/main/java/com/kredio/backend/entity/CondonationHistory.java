package com.kredio.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "condonation_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CondonationHistory {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "loan_id", nullable = false)
    private UUID loanId;

    @Column(name = "schedule_id")
    private UUID scheduleId;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "condoned_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal condonedAmount;

    @Column(name = "condoned_interest", precision = 19, scale = 4)
    private BigDecimal condonedInterest;

    @Column(name = "condoned_principal", precision = 19, scale = 4)
    private BigDecimal condonedPrincipal;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}