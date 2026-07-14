package com.kredio.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "loans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "principal_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal principalAmount;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "term_months", nullable = false)
    private Integer termMonths;

    @Column(name = "opening_date")
    private LocalDate openingDate;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private LoanStatus status;

    @Column(name = "current_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentBalance;

    @Column(name = "approval_status", length = 20)
    @Enumerated(EnumType.STRING)
    private ApprovalStatus approvalStatus;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "interest_rate_id")
    private UUID interestRateId;

    @Column(name = "rate_type")
    private String rateType = "ANNUAL";

    @Column(name = "payment_frequency")
    private String paymentFrequency = "MONTHLY";

    @Column(name = "amortization_method")
    private String amortizationMethod = "FRENCH";

    @Column(name = "is_custom_rate")
    private Boolean isCustomRate;

    @Column(name = "moratory_rate_daily", precision = 5, scale = 4)
    private BigDecimal moratoryRateDaily;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payment_order", columnDefinition = "jsonb")
    private String paymentOrder;

    @Column(name = "guarantee_description", columnDefinition = "TEXT")
    private String guaranteeDescription;

    @Column(name = "references_count")
    private Integer referencesCount;

    @Column(name = "portfolio_id")
    private UUID portfolioId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public enum LoanStatus {
        PENDING, ACTIVE, PAID, DEFAULTED
    }

    public enum ApprovalStatus {
        PENDING, APPROVED, REJECTED
    }
}