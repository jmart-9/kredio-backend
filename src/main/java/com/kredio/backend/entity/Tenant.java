package com.kredio.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tenants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {
    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "subdomain", nullable = false, length = 50, unique = true)
    private String subdomain;

    // ✅ NUEVO: Nombre del schema en PostgreSQL (ej: "tenant_demo", "tenant_a1b2c3d4")
    @Column(name = "schema_name", nullable = false, length = 100, unique = true)
    private String schemaName;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TenantStatus status;

    @Column(name = "country_id")
    private UUID countryId;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "currency_symbol", length = 5)
    private String currencySymbol;

    @Column(name = "plan_id")
    private UUID planId;

    @Column(name = "plan_start_date")
    private LocalDate planStartDate;

    @Column(name = "plan_end_date")
    private LocalDate planEndDate;

    @Column(name = "is_trial")
    private Boolean isTrial;

    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    @Column(name = "payment_status", length = 20)
    private String paymentStatus;

    @Column(name = "notification_90_percent_sent")
    private Boolean notification90PercentSent;

    @Column(name = "custom_branding_enabled")
    private Boolean customBrandingEnabled;

    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public enum TenantStatus {
        ACTIVE, SUSPENDED, CANCELLED
    }
}