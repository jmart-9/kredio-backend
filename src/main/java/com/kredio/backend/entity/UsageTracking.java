package com.kredio.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usage_tracking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageTracking {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "metric_name", nullable = false, length = 50)
    private String metricName;

    @Column(name = "current_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentValue;

    @Column(name = "limit_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal limitValue;

    @Column(name = "percentage_used", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentageUsed;

    @Column(name = "exceeded")
    private Boolean exceeded;

    @CreationTimestamp
    @Column(name = "last_updated", updatable = false)
    private Instant lastUpdated;
}