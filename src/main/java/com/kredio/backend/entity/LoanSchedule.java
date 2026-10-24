package com.kredio.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "loan_schedules", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"loan_id", "installment_number"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanSchedule {
    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "loan_id", nullable = false)
    private UUID loanId;

    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "expected_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal expectedAmount;

    @Column(name = "expected_principal", nullable = false, precision = 19, scale = 4)
    private BigDecimal expectedPrincipal;

    @Column(name = "expected_interest", nullable = false, precision = 19, scale = 4)
    private BigDecimal expectedInterest;

    @Column(name = "paid_amount", precision = 19, scale = 4)
    private BigDecimal paidAmount;

    @Column(name = "moratory_interest", precision = 19, scale = 4)
    private BigDecimal moratoryInterest;

    @Column(name = "paid_date")
    private Instant paidDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ScheduleStatus status;

    // ✅ NUEVOS CAMPOS PARA CONDONACIÓN
    @Column(name = "is_condonated")
    @Builder.Default
    private Boolean isCondonated = false;

    @Column(name = "condonated_amount", precision = 19, scale = 4)
    private BigDecimal condonatedAmount;

    @Column(name = "condonated_interest", precision = 19, scale = 4)
    private BigDecimal condonatedInterest;

    @Column(name = "condonated_at")
    private Instant condonatedAt;

    @Column(name = "condonated_by")
    private UUID condonatedBy;

    @Column(name = "adjusted_principal", precision = 19, scale = 4)
    private BigDecimal adjustedPrincipal;

    @Column(name = "adjusted_interest", precision = 19, scale = 4)
    private BigDecimal adjustedInterest;

    @Column(name = "adjusted_amount", precision = 19, scale = 4)
    private BigDecimal adjustedAmount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public enum ScheduleStatus {
        PENDING,
        PAID,
        OVERDUE,
        PARTIAL,
        CONDONATED
    }
}