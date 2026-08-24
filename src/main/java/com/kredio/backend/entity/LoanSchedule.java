package com.kredio.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
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

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ScheduleStatus status;

    @Column(name = "paid_date")  // ✅ AGREGADO
    private LocalDate paidDate;  // ✅ AGREGADO

    public void setPaidDate(LocalDate paidDate) {
        this.paidDate = paidDate;
    }

    public enum ScheduleStatus {
        PENDING, PAID, OVERDUE
    }
}