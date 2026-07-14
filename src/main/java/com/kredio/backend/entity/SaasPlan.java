package com.kredio.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saas_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaasPlan {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "monthly_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyPrice;

    @Column(name = "trial_days")
    private Integer trialDays;

    @Column(name = "max_users", nullable = false)
    private Integer maxUsers;

    @Column(name = "max_active_loans", nullable = false)
    private Integer maxActiveLoans;

    @Column(name = "max_portfolio_volume", nullable = false, precision = 15, scale = 2)
    private BigDecimal maxPortfolioVolume;

    @Column(name = "max_collectors", nullable = false)
    private Integer maxCollectors;

    @Column(name = "max_clients", nullable = false)
    private Integer maxClients;

    @Column(name = "max_custom_roles", nullable = false)
    private Integer maxCustomRoles;

    @Column(name = "max_custom_interest_rates", nullable = false)
    private Integer maxCustomInterestRates;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "features", columnDefinition = "jsonb")
    private String features;

    @Column(name = "support_level", length = 20)
    private String supportLevel;

    @Column(name = "is_active")
    private Boolean isActive;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}