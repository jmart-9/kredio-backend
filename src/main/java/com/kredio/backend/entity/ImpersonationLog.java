package com.kredio.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "impersonation_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImpersonationLog {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "admin_global_id", nullable = false)
    private UUID adminGlobalId;

    @Column(name = "impersonated_user_id", nullable = false)
    private UUID impersonatedUserId;

    @Column(name = "impersonated_tenant_id", nullable = false)
    private UUID impersonatedTenantId;

    @CreationTimestamp
    @Column(name = "started_at", updatable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "actions_performed", columnDefinition = "jsonb")
    private String actionsPerformed;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;
}