package com.kredio.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "scheduled_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduledReport {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "report_type", nullable = false, length = 50)
    private String reportType;

    @Column(name = "frequency", nullable = false, length = 20)
    private String frequency;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_days", columnDefinition = "jsonb")
    private String customDays;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "email_recipients", nullable = false, columnDefinition = "jsonb")
    private String emailRecipients;

    @Column(name = "format", length = 10)
    private String format;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "last_sent_at")
    private Instant lastSentAt;

    @Column(name = "next_scheduled_at")
    private Instant nextScheduledAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}