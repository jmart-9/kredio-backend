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

    // ✅ CAMPO AGREGADO: Monto pagado de esta cuota
    @Column(name = "paid_amount", precision = 19, scale = 4)
    private BigDecimal paidAmount;

    // ✅ CAMPO AGREGADO: Fecha de pago (Instant para compatibilidad con Java)
    @Column(name = "paid_date")
    private Instant paidDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ScheduleStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public enum ScheduleStatus {
        PENDING,
        PAID,
        OVERDUE,
        PARTIAL  // ✅ ENUM AGREGADO PARA PAGOS PARCIALES
    }
}