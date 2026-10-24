package com.kredio.backend.entity;

import com.kredio.backend.engine.AmortizationMethod;
import com.kredio.backend.engine.DayCountConvention;
import com.kredio.backend.engine.EarlyRepaymentPolicy;
import com.kredio.backend.engine.PaymentFrequency;
import com.kredio.backend.engine.RateType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

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
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "portfolio_id")
    private UUID portfolioId;

    @Column(name = "principal_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal principalAmount;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "rate_type", nullable = false, length = 20)
    private RateType rateType;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_frequency", nullable = false, length = 20)
    private PaymentFrequency paymentFrequency;

    @Enumerated(EnumType.STRING)
    @Column(name = "amortization_method", nullable = false, length = 20)
    private AmortizationMethod amortizationMethod;

    @Column(name = "term_months", nullable = false)
    private Integer termMonths;

    @Column(name = "opening_date", nullable = false)
    private LocalDate openingDate;

    @Column(name = "first_payment_date")
    private LocalDate firstPaymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_count_convention", length = 20)
    private DayCountConvention dayCountConvention;

    @Enumerated(EnumType.STRING)
    @Column(name = "early_repayment_policy", length = 20)
    private EarlyRepaymentPolicy earlyRepaymentPolicy;

    @Builder.Default
    @Column(name = "grace_days")
    private Integer graceDays = 0;

    @Builder.Default
    @Column(name = "adjust_for_business_days")
    private Boolean adjustForBusinessDays = false;

    @Column(name = "current_balance", precision = 19, scale = 4)
    private BigDecimal currentBalance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private LoanStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 20)
    private ApprovalStatus approvalStatus;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Builder.Default
    @Column(name = "is_custom_rate")
    private Boolean isCustomRate = false;

    @Builder.Default
    @Column(name = "moratory_rate_daily", precision = 19, scale = 4)
    private BigDecimal moratoryRateDaily = new BigDecimal("0.0010");

    @Column(name = "guarantee_description", length = 500)
    private String guaranteeDescription;

    @Column(name = "interest_rate_id")
    private UUID interestRateId;

    @Column(name = "payment_order", length = 50)
    private String paymentOrder;

    @Builder.Default
    @Column(name = "references_count")
    private Integer referencesCount = 0;

    // ==========================================
    // ✅ NUEVOS CAMPOS (Fase 1 y Creación de Préstamo)
    // ==========================================
    @Enumerated(EnumType.STRING)
    @Column(name = "disbursement_method", length = 20)
    private DisbursementMethod disbursementMethod;

    @Column(name = "administrative_fee", precision = 19, scale = 4)
    private BigDecimal administrativeFee;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_collection_mode", length = 20)
    private FeeCollectionMode feeCollectionMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "moratory_type", length = 20)
    private MoratoryType moratoryType;

    @Column(name = "moratory_value", precision = 19, scale = 4)
    private BigDecimal moratoryValue;

    @Column(name = "previous_loan_id")
    private UUID previousLoanId;

    @Column(name = "branch_id")
    private UUID branchId;


    // ==========================================
    // Enums internos
    // ==========================================
    public enum LoanStatus {
        PENDING, ACTIVE, DEFAULTED, PAID, REJECTED
    }

    public enum ApprovalStatus {
        PENDING, APPROVED, REJECTED
    }

    public enum DisbursementMethod {
        CASH, TRANSFER, CHECK
    }

    public enum FeeCollectionMode {
        DEDUCTED, FINANCED
    }

    public enum MoratoryType {
        PERCENTAGE, FIXED
    }
}